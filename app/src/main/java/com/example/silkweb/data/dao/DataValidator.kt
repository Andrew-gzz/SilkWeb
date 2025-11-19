package com.example.silkweb.data.dao

import android.util.Patterns
import com.example.silkweb.api.ApiClient
import org.json.JSONObject

object DataValidator {

    //Validaciones sencillas
    fun validatePassword(password: String): String? {
        val pass = password.trim()
        return when{
            pass.isEmpty() -> "La contraseña no puede estar vacía"
            pass.length < 10 -> "La contraseña debe tener al menos 10 caracteres"
            !pass.any { it.isUpperCase() } -> "La contraseña debe contener al menos una letra mayúscula"
            !pass.any { it.isLowerCase() } -> "La contraseña debe contener al menos una letra minúscula"
            !pass.any { it.isDigit() } -> "La contraseña debe contener al menos un número"
            else->null
        }
    }

    fun validatePhone(phone: String): String? {
        val cleanPhone = phone.replace("\\s|-".toRegex(), "")

        return when {
            cleanPhone.isEmpty() -> null
            !cleanPhone.matches(Regex("^\\+?[0-9]{8,15}$")) ->
                "El número de teléfono debe contener entre 8 y 15 dígitos, y solo puede incluir '+' al inicio"
            else -> null
        }
    }

    //Validacion con base de datos (Ya adaptados al nuevo)

    fun isDuplicateUser(username: String): String? {
        val userTrimmed = username.trim()
        when {
            userTrimmed.isEmpty() -> return "El nombre de usuario no puede estar vacío"
            userTrimmed.length < 3 -> return "Debe tener al menos 3 caracteres"
            userTrimmed.length > 20 -> return "No puede tener más de 20 caracteres"
            userTrimmed.contains(" ") -> return "No puede contener espacios"
        }

        var message: String? = null
        val thread = Thread {
            val response = ApiClient.checkUsername(userTrimmed)
            if (response != null) {
                val json = JSONObject(response)
                message = json.getString("message")
            }
        }
        thread.start()
        thread.join()

        return message
    }

    fun isDuplicateEmail(email: String): String? {
        val emailTrimmed = email.trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(emailTrimmed).matches()) {
            return "El email no es valido"
        }

        var message: String? = null
        val thread = Thread {
            val response = ApiClient.checkEmail(emailTrimmed)
            if (response != null) {
                val json = JSONObject(response)
                message = json.getString("message")
            }
        }
        thread.start()
        thread.join()

        return message
    }

    fun idDuplicateFullname(name: String, lastname: String): String?{
        if (name.isEmpty()) return "El nombre no es válido"
        if (lastname.isEmpty()) return "El apellido no es válido"

        var message: String? = null
        val thread = Thread {
            val response = ApiClient.checkFullname(name, lastname)
            if (response != null) {
                val json = JSONObject(response)
                message = json.getString("message")
            }
        }
        thread.start()
        thread.join()

        return message
    }
}