package space.celestia.mobilecelestia

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import space.celestia.mobilecelestia.core.CelestiaAppCore
import space.celestia.mobilecelestia.utils.AppStatusReporter
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CelestiaFragment : Fragment(), GLSurfaceView.Renderer {
    private var activity: Activity? = null

    // MARK: GL View
    private var glViewContainer: FrameLayout? = null
    private var glView: CelestiaView? = null
    private var glViewSize: Size? = null
    private var isReady: Boolean = false

    // MARK: Celestia
    private var pathToLoad: String? = null
    private var cfgToLoad: String? = null
    private var addonToLoad: String? = null
    private val core by lazy { CelestiaAppCore.shared() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            pathToLoad = it.getString(ARG_DATA_DIR)
            cfgToLoad = it.getString(ARG_CFG_FILE)
            addonToLoad = it.getString(ARG_ADDON_DIR)
        }

        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_celestia, container, false)
        glViewContainer = view.findViewById(R.id.celestia_gl_view)
        setupGLView()
        return view
    }

    override fun onPause() {
        super.onPause()

        glView?.onPause()
    }

    override fun onResume() {
        super.onResume()

        glView?.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        activity = context as? Activity
    }

    override fun onDetach() {
        super.onDetach()

        activity = null
    }

    private fun setupGLView() {
        if (activity == null) { return }
        if (glView != null) { return }

        glView = CelestiaView(activity!!)
        glView?.preserveEGLContextOnPause = true
        glView?.setEGLContextClientVersion(2)
        glView?.setRenderer(this)
        glViewContainer?.addView(glView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun loadCelestia(path: String, cfg: String, addon: String?) {
        CelestiaAppCore.chdir(path)

        CelestiaAppCore.setLocaleDirectoryPath("$path/locale", Locale.getDefault().toString())

        val extraDirs = if (addon != null) arrayOf(addon) else null

        if (!core.startSimulation(cfg, extraDirs, AppStatusReporter.shared())) {
            AppStatusReporter.shared().celestiaLoadResult(false)
            return
        }

        if (!core.startRenderer()) {
            AppStatusReporter.shared().celestiaLoadResult(false)
            return
        }

        glViewSize?.let {
            core.resize(it.width, it.height)
            glViewSize = null
        }

        core.tick()
        core.start()

        glView?.isReady = true
        isReady = true

        Log.d(TAG, "Ready to display")
        AppStatusReporter.shared().celestiaLoadResult(true)
    }

    // Render
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        glView?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        glView?.queueEvent {
            // Celestia initialization have to be called with an OpenGL context
            if (pathToLoad != null && cfgToLoad != null) {
                loadCelestia(pathToLoad!!, cfgToLoad!!, addonToLoad)
            }
        }
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        glViewSize = Size(p1, p2)
        Log.d(TAG, "Resize to $p1 x $p2")
        if (!isReady) { return }
        core.resize(p1, p2)
    }

    override fun onDrawFrame(p0: GL10?) {
        if (!isReady) { return }
        core.draw()
        core.tick()
    }

    companion object {
        private const val ARG_DATA_DIR = "data"
        private const val ARG_CFG_FILE = "cfg"
        private const val ARG_ADDON_DIR = "addon"

        private const val TAG = "CelestiaFragment"

        fun newInstance(data: String, cfg: String, addon: String?) =
            CelestiaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATA_DIR, data)
                    putString(ARG_CFG_FILE, cfg)
                    if (addon != null) {
                        putString(ARG_ADDON_DIR, addon)
                    }
                }
            }
    }
}
