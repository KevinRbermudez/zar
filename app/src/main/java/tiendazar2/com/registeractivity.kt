@file:Suppress("DEPRECATION")

package tiendazar2.com

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("ClassName")
class registeractivity : ComponentActivity() {

    private lateinit var mAuth: FirebaseAuth  // Variable para Firebase Authentication
    private lateinit var db: FirebaseFirestore  // Variable para Firestore
    private lateinit var googleSignInClient: GoogleSignInClient  // Variable para Google Sign-In

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registeractivity) // Conecta el diseño XML

        // Inicializa Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.tiendazar2)) // Usar el ID del cliente web
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Obtener las referencias de las vistas (EditText y Button) del archivo XML
        val nameEditText = findViewById<EditText>(R.id.editTextNombreCompleto)
        val emailEditText = findViewById<EditText>(R.id.editTextCorreoElectronico)
        val passwordEditText = findViewById<EditText>(R.id.editTextContrasena)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmarContrasena)
        val registerButton = findViewById<Button>(R.id.buttonRegistro)
        val googleButton = findViewById<Button>(R.id.buttonGoogle)

        // Acción del botón de registro
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validación de que todos los campos no estén vacíos
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación de que las contraseñas coincidan
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Registrar al usuario en Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Si el registro es exitoso, obtener el usuario
                        val user = mAuth.currentUser
                        if (user != null) {
                            // Guardar los datos del usuario en Firestore
                            saveUserData(user.uid, name, email)
                        }
                    } else {
                        // Si ocurre un error en el registro
                        Toast.makeText(this, "Error en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Acción del botón de Google Sign-In
        googleButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            val user = mAuth.currentUser
                            if (user != null) {
                                saveUserData(user.uid, user.displayName ?: "Usuario", user.email ?: "No disponible")
                            }
                        } else {
                            Toast.makeText(this, "Error en Google Sign-In: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función para guardar los datos del usuario en Firestore
    private fun saveUserData(userId: String, name: String, email: String) {
        val user = hashMapOf(
            "name" to name,
            "email" to email
        )

        db.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registro exitoso y datos guardados", Toast.LENGTH_SHORT).show()
                // Redirigir a otra actividad si es necesario
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar los datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}