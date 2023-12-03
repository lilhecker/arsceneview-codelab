package hk.hku.cs.tutorial.ardemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.ARSession
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import java.io.IOException

class ArFragment : Fragment(R.layout.fragment_ar) {

    private lateinit var sceneView: ARSceneView

    private val augmentedImageNodes = mutableListOf<AugmentedImageNode>()
    private val imageName = "augmentedImage"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        addNewAugmentedImage()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById<ARSceneView>(R.id.arSceneView).apply {
            planeRenderer.isEnabled = false
            configureSession { session, config ->
                config.focusMode = Config.FocusMode.AUTO
            }
            onSessionUpdated = { session, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                        val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                            // TODO #3: Add a child ModelNode on the Augmented Image
                        }
                        addChildNode(augmentedImageNode)
                        augmentedImageNodes += augmentedImageNode
                    }
                }
            }
        }
    }

    private fun addNewAugmentedImage() {
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Retrieve the image chosen by the user
                val imageUri = result.data?.data?: return@registerForActivityResult
                val session = sceneView.session?: return@registerForActivityResult
                val config = session.config
                val database = createAugmentedImageDatabaseWithSingleImage(session, imageUri)
                config.augmentedImageDatabase = database
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                session.configure(config)
            }
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        resultLauncher.launch(intent)
    }

    private fun createAugmentedImageDatabaseWithSingleImage(session: ARSession, imageUri: Uri): AugmentedImageDatabase {
        val database = AugmentedImageDatabase(session)
        val bmp = loadAugmentedImageBitmap(imageUri)
        database.addImage(imageName, bmp)
        return database
    }

    private fun loadAugmentedImageBitmap(imageUri: Uri): Bitmap? {
        return try {
            context?.contentResolver?.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "IOException during augmented image load from storage", Toast.LENGTH_LONG).show()
            null
        }
    }
}