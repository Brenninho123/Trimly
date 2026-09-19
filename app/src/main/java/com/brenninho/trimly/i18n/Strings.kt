package com.brenninho.trimly.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.brenninho.trimly.model.Adjustment
import com.brenninho.trimly.model.VideoFilter
import java.util.Locale

enum class AppLanguage(val code: String, val nativeName: String) {
    SYSTEM("system", ""),
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português (Brasil)"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

interface AppStrings {
    val close: String
    val cancel: String
    val clear: String
    val undo: String

    val search: String
    val searchPlaceholder: String
    val settings: String
    val closeSearch: String
    val clearText: String
    val cancelSelection: String
    val selectAll: String
    val addToFavorites: String
    val removeFromFavorites: String
    val removeSelected: String
    fun selectedCount(count: Int): String

    val greetingLate: String
    val greetingMorning: String
    val greetingAfternoon: String
    val greetingEvening: String
    val heroSubtitle: String
    val readingVideo: String
    val selectVideo: String
    val files: String
    val record: String
    val continueEditing: String

    val statVideo: String
    val statVideos: String
    val statTotalTime: String
    val statFavorites: String
    val lessThanMinute: String

    val tips: List<String>

    val toolsTitle: String
    val toolTrim: String
    val toolFilters: String
    val toolEffects: String
    val toolAdjust: String
    val toolRotate: String
    val toolQuality: String
    val toolMerge: String
    val toolSpeed: String
    val toolText: String
    val soon: String
    fun comingSoon(tool: String): String

    val recent: String
    val sort: String
    val layout: String
    val clearRecent: String
    val sortRecent: String
    val sortOldest: String
    val sortNameAsc: String
    val sortNameDesc: String
    val sortLongest: String
    val sortShortest: String
    val layoutList: String
    val layoutGrid: String
    val layoutCompact: String
    val filterAll: String
    val filterShort: String
    val filterMedium: String
    val filterLong: String
    val favorites: String
    val allVideos: String
    val bucketToday: String
    val bucketYesterday: String
    val bucketWeek: String
    val bucketOlder: String

    fun videoCount(count: Int): String
    fun filteredSummary(shown: Int, total: Int): String

    val emptyTitle: String
    val emptySubtitle: String
    fun noMatchQuery(query: String): String
    val noMatchFilters: String
    val tryAnotherFilter: String

    val menuSelect: String
    val menuDetails: String
    val menuRemove: String
    val clearTitle: String
    val clearBody: String
    val clearConfirm: String
    val detailsDuration: String
    val detailsLastOpened: String
    val detailsFavorite: String
    val yes: String
    val no: String
    val noCameraApp: String
    val removedOne: String
    fun removedMany(count: Int): String
    val openFailed: String

    val justNow: String
    fun minutesAgo(count: Long): String
    fun hoursAgo(count: Long): String
    fun daysAgo(count: Long): String

    val settingsTitle: String
    val languageTitle: String
    val languageSystem: String
    val languageHint: String
    val accountTitle: String
    val discordLogin: String
    val discordHint: String
    fun discordSignedInAs(name: String): String
    val discordLogout: String
    val discordWaiting: String
    val discordLoading: String
    val loginErrorCancelled: String
    val loginErrorFailed: String
    val loginErrorNetwork: String
    val loginErrorSecurity: String
    val loginErrorNoBrowser: String
    val homeSection: String
    val tipsTitle: String
    val tipsHint: String
    val aboutTitle: String
    val aboutBody: String
    fun versionLabel(version: String): String

    val editorTitle: String
    val back: String
    val redo: String
    val export: String
    val discardTitle: String
    val discardBody: String
    val discardConfirm: String
    val keepEditing: String
    val play: String
    val pause: String
    val collapseMenu: String
    val expandMenu: String
    val catEdit: String
    val catStyle: String
    val catAudio: String
    val catOutput: String
    val flip: String
    val split: String
    val mute: String
    val unmute: String
    val muted: String
    val volume: String
    val fade: String
    val resetAll: String
    val goToStart: String
    val back5: String
    val forward5: String
    val goToEnd: String
    val loopOn: String
    val loopOff: String
    val lengthLabel: String
    val setStart: String
    val setEnd: String
    val holdCompare: String
    val done: String
    val intensity: String
    val pickOne: String
    val noIntensity: String
    val resetAdjustments: String
    val exporting: String
    val exportingHint: String
    val exportComplete: String
    val share: String
    val openVideo: String
    val exportFailed: String
    val exportFailedHint: String
    val qualityTitle: String
    val qualityOriginal: String
    val qualityUnavailable: String
    fun rotateLabel(degrees: Int): String
    fun startSetTo(time: String): String
    fun endSetTo(time: String): String
    fun chipTrim(time: String): String
    fun removeChip(label: String): String
    fun savedTo(location: String): String
    fun filterName(filter: VideoFilter): String
    fun adjustmentName(kind: Adjustment): String
    val notAVideo: String
    val openingFirstOnly: String
    val exportErrNoSpace: String
    val exportErrUnsupported: String
    val exportErrSource: String
    val exportErrStalled: String
    val exportErrRange: String
    val exportErrEncoder: String
    val exportFastTrim: String
    fun exportEtaSeconds(seconds: Long): String
    fun exportEtaMinutes(minutes: Long): String
    fun exportSize(size: String): String
    fun exportTook(time: String): String
}

object EnglishStrings : AppStrings {
    override val close = "Close"
    override val cancel = "Cancel"
    override val clear = "Clear"
    override val undo = "Undo"

    override val search = "Search"
    override val searchPlaceholder = "Search videos"
    override val settings = "Settings"
    override val closeSearch = "Close search"
    override val clearText = "Clear text"
    override val cancelSelection = "Cancel selection"
    override val selectAll = "Select all"
    override val addToFavorites = "Add to favorites"
    override val removeFromFavorites = "Remove from favorites"
    override val removeSelected = "Remove selected"
    override fun selectedCount(count: Int) = "$count selected"

    override val greetingLate = "Working late?"
    override val greetingMorning = "Good morning"
    override val greetingAfternoon = "Good afternoon"
    override val greetingEvening = "Good evening"
    override val heroSubtitle = "Pick a video to edit, or record a new one"
    override val readingVideo = "Reading video"
    override val selectVideo = "Select video"
    override val files = "Files"
    override val record = "Record"
    override val continueEditing = "Continue editing"

    override val statVideo = "Video"
    override val statVideos = "Videos"
    override val statTotalTime = "Total time"
    override val statFavorites = "Favorites"
    override val lessThanMinute = "<1 min"

    override val tips = listOf(
        "Long-press a video to select several at once",
        "Add videos to Favorites to keep them on top",
        "Use Adjust in the editor to tune brightness and warmth",
        "Export at 720p to get smaller files",
        "Drag the orange handles to set the exact cut"
    )

    override val toolsTitle = "Tools"
    override val toolTrim = "Trim"
    override val toolFilters = "Filters"
    override val toolEffects = "Effects"
    override val toolAdjust = "Adjust"
    override val toolRotate = "Rotate"
    override val toolQuality = "Quality"
    override val toolMerge = "Merge"
    override val toolSpeed = "Speed"
    override val toolText = "Text"
    override val soon = "Soon"
    override fun comingSoon(tool: String) = "$tool is coming soon"

    override val recent = "Recent"
    override val sort = "Sort"
    override val layout = "Layout"
    override val clearRecent = "Clear recent"
    override val sortRecent = "Last opened"
    override val sortOldest = "Oldest opened"
    override val sortNameAsc = "Name A-Z"
    override val sortNameDesc = "Name Z-A"
    override val sortLongest = "Longest first"
    override val sortShortest = "Shortest first"
    override val layoutList = "List"
    override val layoutGrid = "Grid"
    override val layoutCompact = "Compact"
    override val filterAll = "All"
    override val filterShort = "Under 1 min"
    override val filterMedium = "1-5 min"
    override val filterLong = "Over 5 min"
    override val favorites = "Favorites"
    override val allVideos = "All videos"
    override val bucketToday = "Today"
    override val bucketYesterday = "Yesterday"
    override val bucketWeek = "Earlier this week"
    override val bucketOlder = "Older"

    override fun videoCount(count: Int) = if (count == 1) "1 video" else "$count videos"
    override fun filteredSummary(shown: Int, total: Int) = "$shown of $total videos"

    override val emptyTitle = "No recent videos"
    override val emptySubtitle = "Videos you open will show up here"
    override fun noMatchQuery(query: String) = "No videos match \"$query\""
    override val noMatchFilters = "No videos match these filters"
    override val tryAnotherFilter = "Try another filter or clear the search"

    override val menuSelect = "Select"
    override val menuDetails = "Details"
    override val menuRemove = "Remove from recent"
    override val clearTitle = "Clear recent videos?"
    override val clearBody = "This only clears the list. Your videos are not deleted."
    override val clearConfirm = "Clear"
    override val detailsDuration = "Duration"
    override val detailsLastOpened = "Last opened"
    override val detailsFavorite = "Favorite"
    override val yes = "Yes"
    override val no = "No"
    override val noCameraApp = "No camera app found"
    override val removedOne = "Removed from recent"
    override fun removedMany(count: Int) = "$count videos removed"
    override val openFailed = "Could not read this video"

    override val justNow = "Just now"
    override fun minutesAgo(count: Long) = "$count min ago"
    override fun hoursAgo(count: Long) = "$count h ago"
    override fun daysAgo(count: Long) = "$count d ago"

    override val settingsTitle = "Settings"
    override val languageTitle = "Languages"
    override val languageSystem = "System default"
    override val languageHint = "Choose the language of the app"
    override val accountTitle = "Account"
    override val discordLogin = "Login with Discord"
    override val discordHint = "Sign in to show your Discord profile in Trimly"
    override fun discordSignedInAs(name: String) = "Signed in as $name"
    override val discordLogout = "Logout"
    override val discordWaiting = "Waiting for Discord…"
    override val discordLoading = "Loading your profile…"
    override val loginErrorCancelled = "Login was cancelled"
    override val loginErrorFailed = "Login failed. Try again"
    override val loginErrorNetwork = "Could not reach Discord. Check your connection"
    override val loginErrorSecurity = "The login response could not be verified. Try again"
    override val loginErrorNoBrowser = "No browser found to open Discord"
    override val homeSection = "Home"
    override val tipsTitle = "Show tips"
    override val tipsHint = "Display helpful tips on the home screen"
    override val aboutTitle = "About"
    override val aboutBody = "A free video editor that runs entirely on your device. No account, no upload, no watermark."
    override fun versionLabel(version: String) = "Version $version"

    override val editorTitle = "Editor"
    override val back = "Back"
    override val redo = "Redo"
    override val export = "Export"
    override val discardTitle = "Discard changes?"
    override val discardBody = "Your trim and edits will be lost."
    override val discardConfirm = "Discard"
    override val keepEditing = "Keep editing"
    override val play = "Play"
    override val pause = "Pause"
    override val collapseMenu = "Collapse menu"
    override val expandMenu = "Expand menu"
    override val catEdit = "Edit"
    override val catStyle = "Style"
    override val catAudio = "Audio"
    override val catOutput = "Output"
    override val flip = "Flip"
    override val split = "Split"
    override val mute = "Mute"
    override val unmute = "Unmute"
    override val muted = "Muted"
    override val volume = "Volume"
    override val fade = "Fade"
    override val resetAll = "Reset all"
    override val goToStart = "Go to start"
    override val back5 = "Back 5 seconds"
    override val forward5 = "Forward 5 seconds"
    override val goToEnd = "Go to end"
    override val loopOn = "Loop on"
    override val loopOff = "Loop off"
    override val lengthLabel = "Length"
    override val setStart = "Set start"
    override val setEnd = "Set end"
    override val holdCompare = "Hold to compare"
    override val done = "Done"
    override val intensity = "Intensity"
    override val pickOne = "Pick one to apply it"
    override val noIntensity = "This effect has no intensity"
    override val resetAdjustments = "Reset adjustments"
    override val exporting = "Exporting"
    override val exportingHint = "Applying your edits…"
    override val exportComplete = "Export complete"
    override val share = "Share"
    override val openVideo = "Open"
    override val exportFailed = "Export failed"
    override val exportFailedHint = "Something went wrong while exporting. Try again."
    override val qualityTitle = "Export quality"
    override val qualityOriginal = "Original"
    override val qualityUnavailable = "Lower resolutions are not available for this video."
    override fun rotateLabel(degrees: Int) = "Rotate ${degrees}°"
    override fun startSetTo(time: String) = "Start set to $time"
    override fun endSetTo(time: String) = "End set to $time"
    override fun chipTrim(time: String) = "Trim $time"
    override fun removeChip(label: String) = "Remove $label"
    override fun savedTo(location: String) = "Saved to $location"
    override fun filterName(filter: VideoFilter): String = when (filter) {
        VideoFilter.NONE -> "Original"
        VideoFilter.WARM -> "Warm"
        VideoFilter.COOL -> "Cool"
        VideoFilter.VIVID -> "Vivid"
        VideoFilter.FADED -> "Faded"
        VideoFilter.VINTAGE -> "Vintage"
        VideoFilter.SEPIA -> "Sepia"
        VideoFilter.CINEMATIC -> "Cinematic"
        VideoFilter.BLACK_WHITE -> "B&W"
        VideoFilter.NOIR -> "Noir"
        VideoFilter.NEON -> "Neon"
        VideoFilter.DREAM -> "Dream"
        VideoFilter.CHROME -> "Chrome"
        VideoFilter.SUNSET -> "Sunset"
        VideoFilter.FROST -> "Frost"
        VideoFilter.NIGHT_VISION -> "Night"
        VideoFilter.INVERT -> "Invert"
    }
    override fun adjustmentName(kind: Adjustment): String = when (kind) {
        Adjustment.BRIGHTNESS -> "Brightness"
        Adjustment.CONTRAST -> "Contrast"
        Adjustment.SATURATION -> "Saturation"
        Adjustment.WARMTH -> "Warmth"
    }
    override val notAVideo = "That file is not a video"
    override val openingFirstOnly = "Opening the first video only"
    override val exportErrNoSpace = "Not enough free storage. Free some space and try again."
    override val exportErrUnsupported = "This device can't process this video format."
    override val exportErrSource = "The video could not be read. It may have been moved or deleted."
    override val exportErrStalled = "The export stopped responding. Try again."
    override val exportErrRange = "The selected range is too short."
    override val exportErrEncoder = "The video encoder failed. Try a lower quality."
    override val exportFastTrim = "Fast trim: no quality loss"
    override fun exportEtaSeconds(seconds: Long) = "About $seconds s left"
    override fun exportEtaMinutes(minutes: Long) = "About $minutes min left"
    override fun exportSize(size: String) = "Size: $size"
    override fun exportTook(time: String) = "Finished in $time"
}

object PortugueseStrings : AppStrings {
    override val close = "Fechar"
    override val cancel = "Cancelar"
    override val clear = "Limpar"
    override val undo = "Desfazer"

    override val search = "Buscar"
    override val searchPlaceholder = "Buscar vídeos"
    override val settings = "Configurações"
    override val closeSearch = "Fechar busca"
    override val clearText = "Limpar texto"
    override val cancelSelection = "Cancelar seleção"
    override val selectAll = "Selecionar tudo"
    override val addToFavorites = "Adicionar aos favoritos"
    override val removeFromFavorites = "Remover dos favoritos"
    override val removeSelected = "Remover selecionados"
    override fun selectedCount(count: Int) = if (count == 1) "1 selecionado" else "$count selecionados"

    override val greetingLate = "Trabalhando até tarde?"
    override val greetingMorning = "Bom dia"
    override val greetingAfternoon = "Boa tarde"
    override val greetingEvening = "Boa noite"
    override val heroSubtitle = "Escolha um vídeo para editar ou grave um novo"
    override val readingVideo = "Lendo vídeo"
    override val selectVideo = "Selecionar vídeo"
    override val files = "Arquivos"
    override val record = "Gravar"
    override val continueEditing = "Continuar editando"

    override val statVideo = "Vídeo"
    override val statVideos = "Vídeos"
    override val statTotalTime = "Tempo total"
    override val statFavorites = "Favoritos"
    override val lessThanMinute = "<1 min"

    override val tips = listOf(
        "Segure um vídeo para selecionar vários de uma vez",
        "Adicione vídeos aos Favoritos para mantê-los no topo",
        "Use Ajustar no editor para regular brilho e temperatura",
        "Exporte em 720p para gerar arquivos menores",
        "Arraste as alças laranjas para definir o corte exato"
    )

    override val toolsTitle = "Ferramentas"
    override val toolTrim = "Cortar"
    override val toolFilters = "Filtros"
    override val toolEffects = "Efeitos"
    override val toolAdjust = "Ajustar"
    override val toolRotate = "Girar"
    override val toolQuality = "Qualidade"
    override val toolMerge = "Juntar"
    override val toolSpeed = "Velocidade"
    override val toolText = "Texto"
    override val soon = "Em breve"
    override fun comingSoon(tool: String) = "$tool chega em breve"

    override val recent = "Recentes"
    override val sort = "Ordenar"
    override val layout = "Visualização"
    override val clearRecent = "Limpar recentes"
    override val sortRecent = "Último aberto"
    override val sortOldest = "Aberto há mais tempo"
    override val sortNameAsc = "Nome A-Z"
    override val sortNameDesc = "Nome Z-A"
    override val sortLongest = "Mais longos primeiro"
    override val sortShortest = "Mais curtos primeiro"
    override val layoutList = "Lista"
    override val layoutGrid = "Grade"
    override val layoutCompact = "Compacto"
    override val filterAll = "Todos"
    override val filterShort = "Menos de 1 min"
    override val filterMedium = "1-5 min"
    override val filterLong = "Mais de 5 min"
    override val favorites = "Favoritos"
    override val allVideos = "Todos os vídeos"
    override val bucketToday = "Hoje"
    override val bucketYesterday = "Ontem"
    override val bucketWeek = "Esta semana"
    override val bucketOlder = "Mais antigos"

    override fun videoCount(count: Int) = if (count == 1) "1 vídeo" else "$count vídeos"
    override fun filteredSummary(shown: Int, total: Int) = "$shown de $total vídeos"

    override val emptyTitle = "Nenhum vídeo recente"
    override val emptySubtitle = "Os vídeos que você abrir vão aparecer aqui"
    override fun noMatchQuery(query: String) = "Nenhum vídeo corresponde a \"$query\""
    override val noMatchFilters = "Nenhum vídeo corresponde a estes filtros"
    override val tryAnotherFilter = "Tente outro filtro ou limpe a busca"

    override val menuSelect = "Selecionar"
    override val menuDetails = "Detalhes"
    override val menuRemove = "Remover dos recentes"
    override val clearTitle = "Limpar vídeos recentes?"
    override val clearBody = "Isso só limpa a lista. Seus vídeos não são apagados."
    override val clearConfirm = "Limpar"
    override val detailsDuration = "Duração"
    override val detailsLastOpened = "Último acesso"
    override val detailsFavorite = "Favorito"
    override val yes = "Sim"
    override val no = "Não"
    override val noCameraApp = "Nenhum app de câmera encontrado"
    override val removedOne = "Removido dos recentes"
    override fun removedMany(count: Int) = "$count vídeos removidos"
    override val openFailed = "Não foi possível ler este vídeo"

    override val justNow = "Agora mesmo"
    override fun minutesAgo(count: Long) = "há $count min"
    override fun hoursAgo(count: Long) = "há $count h"
    override fun daysAgo(count: Long) = "há $count d"

    override val settingsTitle = "Configurações"
    override val languageTitle = "Idiomas"
    override val languageSystem = "Padrão do sistema"
    override val languageHint = "Escolha o idioma do app"
    override val accountTitle = "Conta"
    override val discordLogin = "Entrar com o Discord"
    override val discordHint = "Entre para mostrar seu perfil do Discord no Trimly"
    override fun discordSignedInAs(name: String) = "Conectado como $name"
    override val discordLogout = "Sair"
    override val discordWaiting = "Aguardando o Discord…"
    override val discordLoading = "Carregando seu perfil…"
    override val loginErrorCancelled = "O login foi cancelado"
    override val loginErrorFailed = "Falha no login. Tente novamente"
    override val loginErrorNetwork = "Não foi possível falar com o Discord. Verifique sua conexão"
    override val loginErrorSecurity = "Não foi possível verificar a resposta do login. Tente novamente"
    override val loginErrorNoBrowser = "Nenhum navegador encontrado para abrir o Discord"
    override val homeSection = "Início"
    override val tipsTitle = "Mostrar dicas"
    override val tipsHint = "Exibe dicas úteis na tela inicial"
    override val aboutTitle = "Sobre"
    override val aboutBody = "Um editor de vídeo gratuito que roda inteiramente no seu aparelho. Sem conta, sem upload, sem marca d'água."
    override fun versionLabel(version: String) = "Versão $version"

    override val editorTitle = "Editor"
    override val back = "Voltar"
    override val redo = "Refazer"
    override val export = "Exportar"
    override val discardTitle = "Descartar alterações?"
    override val discardBody = "Seu corte e suas edições serão perdidos."
    override val discardConfirm = "Descartar"
    override val keepEditing = "Continuar editando"
    override val play = "Reproduzir"
    override val pause = "Pausar"
    override val collapseMenu = "Recolher menu"
    override val expandMenu = "Expandir menu"
    override val catEdit = "Editar"
    override val catStyle = "Estilo"
    override val catAudio = "Áudio"
    override val catOutput = "Saída"
    override val flip = "Espelhar"
    override val split = "Dividir"
    override val mute = "Silenciar"
    override val unmute = "Ativar som"
    override val muted = "Sem som"
    override val volume = "Volume"
    override val fade = "Esmaecer"
    override val resetAll = "Redefinir tudo"
    override val goToStart = "Ir para o início"
    override val back5 = "Voltar 5 segundos"
    override val forward5 = "Avançar 5 segundos"
    override val goToEnd = "Ir para o fim"
    override val loopOn = "Repetição ativada"
    override val loopOff = "Repetição desativada"
    override val lengthLabel = "Duração"
    override val setStart = "Definir início"
    override val setEnd = "Definir fim"
    override val holdCompare = "Segure para comparar"
    override val done = "Concluir"
    override val intensity = "Intensidade"
    override val pickOne = "Escolha um para aplicar"
    override val noIntensity = "Este efeito não tem intensidade"
    override val resetAdjustments = "Redefinir ajustes"
    override val exporting = "Exportando"
    override val exportingHint = "Aplicando suas edições…"
    override val exportComplete = "Exportação concluída"
    override val share = "Compartilhar"
    override val openVideo = "Abrir"
    override val exportFailed = "Falha na exportação"
    override val exportFailedHint = "Algo deu errado ao exportar. Tente novamente."
    override val qualityTitle = "Qualidade da exportação"
    override val qualityOriginal = "Original"
    override val qualityUnavailable = "Resoluções menores não estão disponíveis para este vídeo."
    override fun rotateLabel(degrees: Int) = "Girar ${degrees}°"
    override fun startSetTo(time: String) = "Início definido em $time"
    override fun endSetTo(time: String) = "Fim definido em $time"
    override fun chipTrim(time: String) = "Corte $time"
    override fun removeChip(label: String) = "Remover $label"
    override fun savedTo(location: String) = "Salvo em $location"
    override fun filterName(filter: VideoFilter): String = when (filter) {
        VideoFilter.NONE -> "Original"
        VideoFilter.WARM -> "Quente"
        VideoFilter.COOL -> "Frio"
        VideoFilter.VIVID -> "Vívido"
        VideoFilter.FADED -> "Desbotado"
        VideoFilter.VINTAGE -> "Vintage"
        VideoFilter.SEPIA -> "Sépia"
        VideoFilter.CINEMATIC -> "Cinemático"
        VideoFilter.BLACK_WHITE -> "P&B"
        VideoFilter.NOIR -> "Noir"
        VideoFilter.NEON -> "Neon"
        VideoFilter.DREAM -> "Sonho"
        VideoFilter.CHROME -> "Cromado"
        VideoFilter.SUNSET -> "Entardecer"
        VideoFilter.FROST -> "Gelo"
        VideoFilter.NIGHT_VISION -> "Noturno"
        VideoFilter.INVERT -> "Inverter"
    }
    override fun adjustmentName(kind: Adjustment): String = when (kind) {
        Adjustment.BRIGHTNESS -> "Brilho"
        Adjustment.CONTRAST -> "Contraste"
        Adjustment.SATURATION -> "Saturação"
        Adjustment.WARMTH -> "Temperatura"
    }
    override val notAVideo = "Esse arquivo não é um vídeo"
    override val openingFirstOnly = "Abrindo só o primeiro vídeo"
    override val exportErrNoSpace = "Espaço livre insuficiente. Libere espaço e tente novamente."
    override val exportErrUnsupported = "Este aparelho não consegue processar o formato deste vídeo."
    override val exportErrSource = "Não foi possível ler o vídeo. Ele pode ter sido movido ou apagado."
    override val exportErrStalled = "A exportação parou de responder. Tente novamente."
    override val exportErrRange = "O trecho selecionado é curto demais."
    override val exportErrEncoder = "O codificador de vídeo falhou. Tente uma qualidade menor."
    override val exportFastTrim = "Corte rápido: sem perda de qualidade"
    override fun exportEtaSeconds(seconds: Long) = "Faltam cerca de $seconds s"
    override fun exportEtaMinutes(minutes: Long) = "Faltam cerca de $minutes min"
    override fun exportSize(size: String) = "Tamanho: $size"
    override fun exportTook(time: String) = "Concluído em $time"
}

object SpanishStrings : AppStrings {
    override val close = "Cerrar"
    override val cancel = "Cancelar"
    override val clear = "Borrar"
    override val undo = "Deshacer"

    override val search = "Buscar"
    override val searchPlaceholder = "Buscar videos"
    override val settings = "Ajustes"
    override val closeSearch = "Cerrar búsqueda"
    override val clearText = "Borrar texto"
    override val cancelSelection = "Cancelar selección"
    override val selectAll = "Seleccionar todo"
    override val addToFavorites = "Añadir a favoritos"
    override val removeFromFavorites = "Quitar de favoritos"
    override val removeSelected = "Quitar seleccionados"
    override fun selectedCount(count: Int) = if (count == 1) "1 seleccionado" else "$count seleccionados"

    override val greetingLate = "¿Trabajando hasta tarde?"
    override val greetingMorning = "Buenos días"
    override val greetingAfternoon = "Buenas tardes"
    override val greetingEvening = "Buenas noches"
    override val heroSubtitle = "Elige un video para editar o graba uno nuevo"
    override val readingVideo = "Leyendo video"
    override val selectVideo = "Seleccionar video"
    override val files = "Archivos"
    override val record = "Grabar"
    override val continueEditing = "Seguir editando"

    override val statVideo = "Video"
    override val statVideos = "Videos"
    override val statTotalTime = "Tiempo total"
    override val statFavorites = "Favoritos"
    override val lessThanMinute = "<1 min"

    override val tips = listOf(
        "Mantén pulsado un video para seleccionar varios a la vez",
        "Añade videos a Favoritos para tenerlos siempre arriba",
        "Usa Ajustar en el editor para regular brillo y calidez",
        "Exporta en 720p para obtener archivos más pequeños",
        "Arrastra los controles naranjas para fijar el corte exacto"
    )

    override val toolsTitle = "Herramientas"
    override val toolTrim = "Recortar"
    override val toolFilters = "Filtros"
    override val toolEffects = "Efectos"
    override val toolAdjust = "Ajustar"
    override val toolRotate = "Girar"
    override val toolQuality = "Calidad"
    override val toolMerge = "Unir"
    override val toolSpeed = "Velocidad"
    override val toolText = "Texto"
    override val soon = "Pronto"
    override fun comingSoon(tool: String) = "$tool llegará pronto"

    override val recent = "Recientes"
    override val sort = "Ordenar"
    override val layout = "Vista"
    override val clearRecent = "Borrar recientes"
    override val sortRecent = "Último abierto"
    override val sortOldest = "Abierto hace más tiempo"
    override val sortNameAsc = "Nombre A-Z"
    override val sortNameDesc = "Nombre Z-A"
    override val sortLongest = "Más largos primero"
    override val sortShortest = "Más cortos primero"
    override val layoutList = "Lista"
    override val layoutGrid = "Cuadrícula"
    override val layoutCompact = "Compacto"
    override val filterAll = "Todos"
    override val filterShort = "Menos de 1 min"
    override val filterMedium = "1-5 min"
    override val filterLong = "Más de 5 min"
    override val favorites = "Favoritos"
    override val allVideos = "Todos los videos"
    override val bucketToday = "Hoy"
    override val bucketYesterday = "Ayer"
    override val bucketWeek = "Esta semana"
    override val bucketOlder = "Anteriores"

    override fun videoCount(count: Int) = if (count == 1) "1 video" else "$count videos"
    override fun filteredSummary(shown: Int, total: Int) = "$shown de $total videos"

    override val emptyTitle = "Sin videos recientes"
    override val emptySubtitle = "Los videos que abras aparecerán aquí"
    override fun noMatchQuery(query: String) = "Ningún video coincide con \"$query\""
    override val noMatchFilters = "Ningún video coincide con estos filtros"
    override val tryAnotherFilter = "Prueba otro filtro o borra la búsqueda"

    override val menuSelect = "Seleccionar"
    override val menuDetails = "Detalles"
    override val menuRemove = "Quitar de recientes"
    override val clearTitle = "¿Borrar videos recientes?"
    override val clearBody = "Solo se borra la lista. Tus videos no se eliminan."
    override val clearConfirm = "Borrar"
    override val detailsDuration = "Duración"
    override val detailsLastOpened = "Último acceso"
    override val detailsFavorite = "Favorito"
    override val yes = "Sí"
    override val no = "No"
    override val noCameraApp = "No se encontró ninguna app de cámara"
    override val removedOne = "Quitado de recientes"
    override fun removedMany(count: Int) = "$count videos quitados"
    override val openFailed = "No se pudo leer este video"

    override val justNow = "Ahora mismo"
    override fun minutesAgo(count: Long) = "hace $count min"
    override fun hoursAgo(count: Long) = "hace $count h"
    override fun daysAgo(count: Long) = "hace $count d"

    override val settingsTitle = "Ajustes"
    override val languageTitle = "Idiomas"
    override val languageSystem = "Predeterminado del sistema"
    override val languageHint = "Elige el idioma de la app"
    override val accountTitle = "Cuenta"
    override val discordLogin = "Iniciar sesión con Discord"
    override val discordHint = "Inicia sesión para mostrar tu perfil de Discord en Trimly"
    override fun discordSignedInAs(name: String) = "Sesión iniciada como $name"
    override val discordLogout = "Cerrar sesión"
    override val discordWaiting = "Esperando a Discord…"
    override val discordLoading = "Cargando tu perfil…"
    override val loginErrorCancelled = "Se canceló el inicio de sesión"
    override val loginErrorFailed = "Error al iniciar sesión. Inténtalo de nuevo"
    override val loginErrorNetwork = "No se pudo conectar con Discord. Revisa tu conexión"
    override val loginErrorSecurity = "No se pudo verificar la respuesta del inicio de sesión. Inténtalo de nuevo"
    override val loginErrorNoBrowser = "No se encontró ningún navegador para abrir Discord"
    override val homeSection = "Inicio"
    override val tipsTitle = "Mostrar consejos"
    override val tipsHint = "Muestra consejos útiles en la pantalla de inicio"
    override val aboutTitle = "Acerca de"
    override val aboutBody = "Un editor de video gratuito que funciona por completo en tu dispositivo. Sin cuenta, sin subidas, sin marca de agua."
    override fun versionLabel(version: String) = "Versión $version"

    override val editorTitle = "Editor"
    override val back = "Atrás"
    override val redo = "Rehacer"
    override val export = "Exportar"
    override val discardTitle = "¿Descartar cambios?"
    override val discardBody = "Se perderán tu recorte y tus ediciones."
    override val discardConfirm = "Descartar"
    override val keepEditing = "Seguir editando"
    override val play = "Reproducir"
    override val pause = "Pausar"
    override val collapseMenu = "Contraer menú"
    override val expandMenu = "Expandir menú"
    override val catEdit = "Editar"
    override val catStyle = "Estilo"
    override val catAudio = "Audio"
    override val catOutput = "Salida"
    override val flip = "Voltear"
    override val split = "Dividir"
    override val mute = "Silenciar"
    override val unmute = "Activar sonido"
    override val muted = "Sin sonido"
    override val volume = "Volumen"
    override val fade = "Fundido"
    override val resetAll = "Restablecer todo"
    override val goToStart = "Ir al inicio"
    override val back5 = "Retroceder 5 segundos"
    override val forward5 = "Avanzar 5 segundos"
    override val goToEnd = "Ir al final"
    override val loopOn = "Repetición activada"
    override val loopOff = "Repetición desactivada"
    override val lengthLabel = "Duración"
    override val setStart = "Fijar inicio"
    override val setEnd = "Fijar final"
    override val holdCompare = "Mantén para comparar"
    override val done = "Listo"
    override val intensity = "Intensidad"
    override val pickOne = "Elige uno para aplicarlo"
    override val noIntensity = "Este efecto no tiene intensidad"
    override val resetAdjustments = "Restablecer ajustes"
    override val exporting = "Exportando"
    override val exportingHint = "Aplicando tus ediciones…"
    override val exportComplete = "Exportación completada"
    override val share = "Compartir"
    override val openVideo = "Abrir"
    override val exportFailed = "Error al exportar"
    override val exportFailedHint = "Algo salió mal al exportar. Inténtalo de nuevo."
    override val qualityTitle = "Calidad de exportación"
    override val qualityOriginal = "Original"
    override val qualityUnavailable = "No hay resoluciones menores disponibles para este video."
    override fun rotateLabel(degrees: Int) = "Girar ${degrees}°"
    override fun startSetTo(time: String) = "Inicio fijado en $time"
    override fun endSetTo(time: String) = "Final fijado en $time"
    override fun chipTrim(time: String) = "Recorte $time"
    override fun removeChip(label: String) = "Quitar $label"
    override fun savedTo(location: String) = "Guardado en $location"
    override fun filterName(filter: VideoFilter): String = when (filter) {
        VideoFilter.NONE -> "Original"
        VideoFilter.WARM -> "Cálido"
        VideoFilter.COOL -> "Frío"
        VideoFilter.VIVID -> "Vívido"
        VideoFilter.FADED -> "Desvaído"
        VideoFilter.VINTAGE -> "Vintage"
        VideoFilter.SEPIA -> "Sepia"
        VideoFilter.CINEMATIC -> "Cinemático"
        VideoFilter.BLACK_WHITE -> "B/N"
        VideoFilter.NOIR -> "Noir"
        VideoFilter.NEON -> "Neón"
        VideoFilter.DREAM -> "Sueño"
        VideoFilter.CHROME -> "Cromo"
        VideoFilter.SUNSET -> "Atardecer"
        VideoFilter.FROST -> "Escarcha"
        VideoFilter.NIGHT_VISION -> "Nocturno"
        VideoFilter.INVERT -> "Invertir"
    }
    override fun adjustmentName(kind: Adjustment): String = when (kind) {
        Adjustment.BRIGHTNESS -> "Brillo"
        Adjustment.CONTRAST -> "Contraste"
        Adjustment.SATURATION -> "Saturación"
        Adjustment.WARMTH -> "Calidez"
    }
    override val notAVideo = "Ese archivo no es un video"
    override val openingFirstOnly = "Abriendo solo el primer video"
    override val exportErrNoSpace = "No hay suficiente espacio libre. Libera espacio e inténtalo de nuevo."
    override val exportErrUnsupported = "Este dispositivo no puede procesar el formato de este video."
    override val exportErrSource = "No se pudo leer el video. Puede que se haya movido o eliminado."
    override val exportErrStalled = "La exportación dejó de responder. Inténtalo de nuevo."
    override val exportErrRange = "El tramo seleccionado es demasiado corto."
    override val exportErrEncoder = "El codificador de video falló. Prueba una calidad menor."
    override val exportFastTrim = "Recorte rápido: sin pérdida de calidad"
    override fun exportEtaSeconds(seconds: Long) = "Faltan unos $seconds s"
    override fun exportEtaMinutes(minutes: Long) = "Faltan unos $minutes min"
    override fun exportSize(size: String) = "Tamaño: $size"
    override fun exportTook(time: String) = "Terminado en $time"
}

fun stringsFor(language: AppLanguage): AppStrings {
    val effective = if (language == AppLanguage.SYSTEM) {
        when (Locale.getDefault().language) {
            "pt" -> AppLanguage.PORTUGUESE
            "es" -> AppLanguage.SPANISH
            else -> AppLanguage.ENGLISH
        }
    } else {
        language
    }
    return when (effective) {
        AppLanguage.PORTUGUESE -> PortugueseStrings
        AppLanguage.SPANISH -> SpanishStrings
        else -> EnglishStrings
    }
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
