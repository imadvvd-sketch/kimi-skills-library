package app.imadnote

import android.app.Application

class ImadNoteApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
