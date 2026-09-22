package com.brenninho.trimly.desktop

import com.brenninho.trimly.platform.Platform
import com.brenninho.trimly.platform.PlatformKind
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val DISCORD_CLIENT_ID = "1540653184530251847"
private const val OP_HANDSHAKE = 0
private const val OP_FRAME = 1
private const val PIPE_ATTEMPTS = 10
private const val RETRY_DELAY_MS = 15_000L
private const val REFRESH_DELAY_MS = 45_000L

private interface IpcTransport {
    fun write(data: ByteArray)
    fun read(size: Int): ByteArray
    fun close()
}

private class WindowsPipeTransport(private val file: RandomAccessFile) : IpcTransport {
    override fun write(data: ByteArray) = file.write(data)
    override fun read(size: Int): ByteArray {
        val buffer = ByteArray(size)
        file.readFully(buffer)
        return buffer
    }
    override fun close() = file.close()
}

private class UnixSocketTransport(private val channel: SocketChannel) : IpcTransport {
    override fun write(data: ByteArray) {
        channel.write(ByteBuffer.wrap(data))
    }
    override fun read(size: Int): ByteArray {
        val buffer = ByteBuffer.allocate(size)
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) throw IOException("Discord IPC closed the connection")
        }
        return buffer.array()
    }
    override fun close() = channel.close()
}

object DiscordRpc {

    private val lock = Mutex()
    private var transport: IpcTransport? = null
    private var loopJob: Job? = null

    var isConnected: Boolean = false
        private set

    fun start(scope: CoroutineScope) {
        if (loopJob != null) return
        loopJob = scope.launch(Dispatchers.IO) {
            while (true) {
                if (!isConnected) tryConnect()
                delay(if (isConnected) REFRESH_DELAY_MS else RETRY_DELAY_MS)
            }
        }
    }

    suspend fun setBrowsing() {
        setActivity(details = "Browsing videos", state = null, startedAtMs = null)
    }

    suspend fun setEditing(fileName: String, startedAtMs: Long) {
        setActivity(details = "Editing a video", state = fileName, startedAtMs = startedAtMs)
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            lock.withLock {
                val current = transport ?: return@withLock
                val args = JSONObject().put("pid", ProcessHandle.current().pid()).put("activity", JSONObject.NULL)
                sendOrDisconnect(current, "SET_ACTIVITY", args)
            }
        }
    }

    fun shutdown() {
        loopJob?.cancel()
        loopJob = null
        transport?.let { runCatching { it.close() } }
        transport = null
        isConnected = false
    }

    private suspend fun setActivity(details: String, state: String?, startedAtMs: Long?) {
        withContext(Dispatchers.IO) {
            lock.withLock {
                val current = transport ?: return@withLock
                val activity = JSONObject().put("details", details)
                if (state != null) activity.put("state", state)
                if (startedAtMs != null) {
                    activity.put("timestamps", JSONObject().put("start", startedAtMs))
                }
                activity.put(
                    "assets",
                    JSONObject().put("large_image", "trimly_logo").put("large_text", "Trimly")
                )
                val args = JSONObject()
                    .put("pid", ProcessHandle.current().pid())
                    .put("activity", activity)
                sendOrDisconnect(current, "SET_ACTIVITY", args)
            }
        }
    }

    private fun sendOrDisconnect(current: IpcTransport, cmd: String, args: JSONObject) {
        val ok = runCatching { sendFrame(current, cmd, args) }.isSuccess
        if (!ok) {
            runCatching { current.close() }
            transport = null
            isConnected = false
        }
    }

    private fun sendFrame(transport: IpcTransport, cmd: String, args: JSONObject) {
        val payload = JSONObject()
            .put("cmd", cmd)
            .put("args", args)
            .put("nonce", UUID.randomUUID().toString())
        writeOp(transport, OP_FRAME, payload.toString())
        readFrame(transport)
    }

    private suspend fun tryConnect() {
        withContext(Dispatchers.IO) {
            lock.withLock {
                val candidate = openTransport()
                if (candidate == null) {
                    isConnected = false
                    return@withLock
                }
                val handshake = JSONObject().put("v", 1).put("client_id", DISCORD_CLIENT_ID)
                val success = runCatching {
                    writeOp(candidate, OP_HANDSHAKE, handshake.toString())
                    readFrame(candidate)
                }.isSuccess
                if (success) {
                    transport = candidate
                    isConnected = true
                } else {
                    runCatching { candidate.close() }
                    isConnected = false
                }
            }
        }
    }

    private fun openTransport(): IpcTransport? = when (Platform.kind) {
        PlatformKind.WINDOWS -> openWindowsPipe()
        else -> openUnixSocket()
    }

    private fun openWindowsPipe(): IpcTransport? {
        for (index in 0 until PIPE_ATTEMPTS) {
            val file = runCatching {
                RandomAccessFile("\\\\.\\pipe\\discord-ipc-$index", "rw")
            }.getOrNull()
            if (file != null) return WindowsPipeTransport(file)
        }
        return null
    }

    private fun openUnixSocket(): IpcTransport? {
        val base = System.getenv("XDG_RUNTIME_DIR")
            ?: System.getenv("TMPDIR")
            ?: System.getenv("TMP")
            ?: System.getenv("TEMP")
            ?: "/tmp"
        for (index in 0 until PIPE_ATTEMPTS) {
            val channel = runCatching {
                val path = File(base, "discord-ipc-$index").toPath()
                SocketChannel.open(StandardProtocolFamily.UNIX).apply {
                    connect(UnixDomainSocketAddress.of(path))
                }
            }.getOrNull()
            if (channel != null) return UnixSocketTransport(channel)
        }
        return null
    }

    private fun writeOp(transport: IpcTransport, opcode: Int, json: String) {
        val payload = json.toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(opcode)
        header.putInt(payload.size)
        transport.write(header.array())
        transport.write(payload)
    }

    private fun readFrame(transport: IpcTransport): String {
        val header = ByteBuffer.wrap(transport.read(8)).order(ByteOrder.LITTLE_ENDIAN)
        header.getInt()
        val length = header.getInt()
        return String(transport.read(length), Charsets.UTF_8)
    }
}
