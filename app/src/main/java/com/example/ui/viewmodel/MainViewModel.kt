package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TrainingRepository
import com.example.data.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val user: UserEntity) : AuthState
    data class Error(val message: String) : AuthState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository: TrainingRepository

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _authState
        .map { if (it is AuthState.Authenticated) it.user else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Flow of all users for Admin User CRUD
    val allUsers: StateFlow<List<UserEntity>> by lazy {
        repository.getAllUsers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Flow of all courses for catalog & admin course list
    val allCourses: StateFlow<List<CourseEntity>> by lazy {
        repository.getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Platform Statistics for Admin & Dashboard
    val totalUserCount: StateFlow<Int> by lazy {
        repository.getTotalUserCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    val totalStudentCount: StateFlow<Int> by lazy {
        repository.getStudentCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    val totalCourseCount: StateFlow<Int> by lazy {
        repository.getCourseCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = TrainingRepository(
            userDao = db.userDao(),
            courseDao = db.courseDao(),
            moduleDao = db.moduleDao(),
            quizDao = db.quizDao(),
            progressDao = db.progressDao()
        )
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim().lowercase())
            if (user == null) {
                onResult(false, "User not found. Please check your email.")
                return@launch
            }
            if (!user.isActive) {
                onResult(false, "This user account has been deactivated by an admin.")
                return@launch
            }
            if (user.passwordHash != password) {
                onResult(false, "Incorrect password. Please try again.")
                return@launch
            }
            _authState.value = AuthState.Authenticated(user)
            onResult(true, null)
        }
    }

    fun quickLoginAs(role: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val email = when (role) {
                "ADMIN" -> "admin@corporate.com"
                "EMPLOYEE" -> "john.doe@techcorp.com"
                else -> "sarah.chen@techcorp.com"
            }
            val user = repository.getUserByEmail(email)
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        role: String,
        department: String,
        designation: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val initials = fullName.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifEmpty { "CU" }

            val newUser = UserEntity(
                email = email.trim().lowercase(),
                passwordHash = password,
                fullName = fullName.trim(),
                role = role,
                department = department.trim(),
                designation = designation.trim(),
                avatarInitials = initials,
                isActive = true
            )

            val result = repository.registerUser(newUser)
            if (result.isSuccess) {
                val createdUser = newUser.copy(id = result.getOrThrow().toInt())
                _authState.value = AuthState.Authenticated(createdUser)
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Unauthenticated
    }

    // --- USER CRUD OPERATIONS (ADMIN) ---
    fun adminCreateUser(user: UserEntity, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.registerUser(user)
            if (res.isSuccess) {
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun adminUpdateUser(user: UserEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateUser(user)
            // If editing current logged in user, refresh local state
            val current = currentUser.value
            if (current != null && current.id == user.id) {
                _authState.value = AuthState.Authenticated(user)
            }
            onComplete()
        }
    }

    fun adminDeleteUser(userId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            val current = currentUser.value
            if (current != null && current.id == userId) {
                logout()
            }
            onComplete()
        }
    }

    // --- COURSE CRUD OPERATIONS (ADMIN) ---
    fun adminSaveCourse(course: CourseEntity, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            if (course.id == 0) {
                val newId = repository.insertCourse(course).toInt()
                onComplete(newId)
            } else {
                repository.updateCourse(course)
                onComplete(course.id)
            }
        }
    }

    fun adminDeleteCourse(courseId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            onComplete()
        }
    }

    // --- MODULE CRUD OPERATIONS (ADMIN) ---
    fun adminSaveModule(module: ModuleEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (module.id == 0) {
                repository.insertModule(module)
            } else {
                repository.updateModule(module)
            }
            onComplete()
        }
    }

    fun adminDeleteModule(moduleId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteModule(moduleId)
            onComplete()
        }
    }

    // --- QUIZ CRUD OPERATIONS (ADMIN) ---
    fun adminSaveQuestion(question: QuizQuestionEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (question.id == 0) {
                repository.insertQuestion(question)
            } else {
                repository.updateQuestion(question)
            }
            onComplete()
        }
    }

    fun adminDeleteQuestion(questionId: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteQuestion(questionId)
            onComplete()
        }
    }

    // --- STUDENT PROGRESS & QUIZ ATTEMPT ---
    fun markModuleCompleted(courseId: Int, moduleId: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markModuleCompleted(
                userId = user.id,
                courseId = courseId,
                moduleId = moduleId
            )
        }
    }

    fun submitQuiz(
        courseId: Int,
        moduleId: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        onComplete: (QuizAttemptEntity) -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
            val passed = percentage >= 70

            val attempt = QuizAttemptEntity(
                userId = user.id,
                courseId = courseId,
                moduleId = moduleId,
                scorePercentage = percentage,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                passed = passed,
                attemptedAt = System.currentTimeMillis()
            )

            repository.recordQuizAttempt(attempt)

            if (passed) {
                repository.markModuleCompleted(user.id, courseId, moduleId)
            }

            onComplete(attempt)
        }
    }
}
