package com.example.silkweb.data.dao

import android.util.Patterns

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

    //Validacion con base de datos

    fun isDuplicateUser(username: String): String? {
        var msj: String? = null
        val userTrimmed = username.trim()

        // Validaciones básicas
        when {
            userTrimmed.isEmpty() -> return "El nombre de usuario no puede estar vacío"
            userTrimmed.length < 3 -> return "Debe tener al menos 3 caracteres"
            userTrimmed.length > 20 -> return "No puede tener más de 20 caracteres"
            userTrimmed.contains(" ") -> return "No puede contener espacios"
        }

        // Validación en segundo plano
        val thread = Thread {
                msj = UserDao.checkUserExistsSP(username)
        }

        thread.start()
        thread.join() // Espera a que termine el hilo (⚠️ bloquea el hilo actual si es el UI thread)

        return msj
    }

    fun isDuplicateEmail(email: String): String? {
        var msj: String? = null
        val emailTrimmed = email.trim()

        // Validaciones básicas
        when {
            !(email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches())-> return "El email no es valido"
        }

        // Validación en segundo plano
        val thread = Thread {
            msj = UserDao.checkEmailExistsSP(emailTrimmed)
        }

        thread.start()
        thread.join()

        return msj
    }

    fun idDuplicateFullname(name: String, lastname: String): String?{
        var msj: String? = null

        // Validaciones básicas
        when {
            !(name.isNotEmpty())-> return "El nombre no es válido"
            !(lastname.isNotEmpty())->return "El apellido no es válido"
        }
        // Validación en segundo plano
        val thread = Thread {
            try{
                msj = UserDao.checkFullNameExistsSP(name, lastname)
            }catch (e: Exception){
                msj = e.message
            }

        }

        thread.start()
        thread.join()

        return msj
    }
}