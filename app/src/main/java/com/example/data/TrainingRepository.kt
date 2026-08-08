package com.example.data

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class TrainingRepository(
    private val userDao: UserDao,
    private val courseDao: CourseDao,
    private val moduleDao: ModuleDao,
    private val quizDao: QuizDao,
    private val progressDao: ProgressDao
) {
    // --- User Management & CRUD ---
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    
    suspend fun getUserById(id: Int): UserEntity? = userDao.getUserById(id)
    
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    
    suspend fun registerUser(user: UserEntity): Result<Long> {
        val existing = userDao.getUserByEmail(user.email)
        if (existing != null) {
            return Result.failure(Exception("Account with this email already exists."))
        }
        val id = userDao.insertUser(user)
        return Result.success(id)
    }

    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    suspend fun deleteUser(id: Int) = userDao.deleteUserById(id)

    fun getTotalUserCount(): Flow<Int> = userDao.getTotalUserCount()
    fun getStudentCount(): Flow<Int> = userDao.getStudentCount()

    // --- Course Operations & CRUD ---
    fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAllCourses()

    fun getCoursesByCategory(category: String): Flow<List<CourseEntity>> =
        courseDao.getCoursesByCategory(category)

    fun getCourseById(id: Int): Flow<CourseEntity?> = courseDao.getCourseById(id)

    suspend fun getCourseByIdDirect(id: Int): CourseEntity? = courseDao.getCourseByIdDirect(id)

    suspend fun insertCourse(course: CourseEntity): Long = courseDao.insertCourse(course)

    suspend fun updateCourse(course: CourseEntity) = courseDao.updateCourse(course)

    suspend fun deleteCourse(id: Int) = courseDao.deleteCourseById(id)

    fun getCourseCount(): Flow<Int> = courseDao.getCourseCount()

    // --- Module Operations & CRUD ---
    fun getModulesForCourse(courseId: Int): Flow<List<ModuleEntity>> =
        moduleDao.getModulesForCourse(courseId)

    suspend fun getModulesForCourseDirect(courseId: Int): List<ModuleEntity> =
        moduleDao.getModulesForCourseDirect(courseId)

    fun getModuleById(id: Int): Flow<ModuleEntity?> = moduleDao.getModuleById(id)

    suspend fun getModuleByIdDirect(id: Int): ModuleEntity? = moduleDao.getModuleByIdDirect(id)

    suspend fun insertModule(module: ModuleEntity): Long = moduleDao.insertModule(module)

    suspend fun updateModule(module: ModuleEntity) = moduleDao.updateModule(module)

    suspend fun deleteModule(id: Int) = moduleDao.deleteModuleById(id)

    // --- Quiz Operations & CRUD ---
    fun getQuestionsForModule(moduleId: Int): Flow<List<QuizQuestionEntity>> =
        quizDao.getQuestionsForModule(moduleId)

    suspend fun getQuestionsForModuleDirect(moduleId: Int): List<QuizQuestionEntity> =
        quizDao.getQuestionsForModuleDirect(moduleId)

    suspend fun insertQuestion(question: QuizQuestionEntity): Long = quizDao.insertQuestion(question)

    suspend fun updateQuestion(question: QuizQuestionEntity) = quizDao.updateQuestion(question)

    suspend fun deleteQuestion(id: Int) = quizDao.deleteQuestionById(id)

    suspend fun recordQuizAttempt(attempt: QuizAttemptEntity): Long = quizDao.insertAttempt(attempt)

    fun getAttemptsForUser(userId: Int): Flow<List<QuizAttemptEntity>> =
        quizDao.getAttemptsForUser(userId)

    fun getAverageScoreForUser(userId: Int): Flow<Double?> =
        quizDao.getAverageScoreForUser(userId)

    // --- User Progress ---
    fun getUserProgress(userId: Int): Flow<List<UserProgressEntity>> =
        progressDao.getUserProgress(userId)

    fun getUserProgressForCourse(userId: Int, courseId: Int): Flow<List<UserProgressEntity>> =
        progressDao.getUserProgressForCourse(userId, courseId)

    suspend fun markModuleCompleted(userId: Int, courseId: Int, moduleId: Int) {
        val progress = UserProgressEntity(
            userId = userId,
            courseId = courseId,
            moduleId = moduleId,
            isModuleCompleted = true,
            lastUpdated = System.currentTimeMillis()
        )
        progressDao.saveProgress(progress)
    }

    fun getCompletedModulesCount(userId: Int): Flow<Int> =
        progressDao.getCompletedModulesCount(userId)

    fun getEnrolledCoursesCount(userId: Int): Flow<Int> =
        progressDao.getEnrolledCoursesCount(userId)
}
