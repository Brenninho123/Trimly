package com.brenninho.trimly.i18n

import androidx.compose.runtime.staticCompositionLocalOf
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
