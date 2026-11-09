package com.example.questionbank

import android.app.Application
import com.example.questionbank.database.QuestionDatabase

class QuestionBankApp : Application() {
    val database by lazy { QuestionDatabase.getDatabase(this) }
}
