package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CourseEntity::class,
        ModuleEntity::class,
        QuizQuestionEntity::class,
        UserProgressEntity::class,
        QuizAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun moduleDao(): ModuleDao
    abstract fun quizDao(): QuizDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "corporate_training_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val userDao = db.userDao()
            val courseDao = db.courseDao()
            val moduleDao = db.moduleDao()
            val quizDao = db.quizDao()

            // Seed Users
            val adminId = userDao.insertUser(
                UserEntity(
                    email = "admin@corporate.com",
                    passwordHash = "admin123",
                    fullName = "Elena Vance",
                    role = "ADMIN",
                    department = "Corporate L&D",
                    designation = "Global Training Director",
                    avatarInitials = "EV"
                )
            )

            val empId = userDao.insertUser(
                UserEntity(
                    email = "john.doe@techcorp.com",
                    passwordHash = "employee123",
                    fullName = "John Doe",
                    role = "EMPLOYEE",
                    department = "Hardware Systems",
                    designation = "Lead Hardware Engineer",
                    avatarInitials = "JD"
                )
            )

            val studentId = userDao.insertUser(
                UserEntity(
                    email = "sarah.chen@techcorp.com",
                    passwordHash = "student123",
                    fullName = "Sarah Chen",
                    role = "ENGINEER_STUDENT",
                    department = "Embedded Firmware",
                    designation = "Junior Electronics Engineer",
                    avatarInitials = "SC"
                )
            )

            // --- COURSE 1: Soft Skills - Corporate Leadership ---
            val c1Id = courseDao.insertCourse(
                CourseEntity(
                    title = "Executive Leadership & Team Alignment",
                    category = "SOFT_SKILLS",
                    description = "Master core leadership strategies, team motivation, cross-functional alignment, and executive decision-making under uncertainty.",
                    thumbnailKey = "leadership",
                    level = "Intermediate",
                    estimatedHours = 4,
                    instructorName = "Dr. Marcus Sterling"
                )
            ).toInt()

            val m1_1Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c1Id,
                    moduleOrder = 1,
                    title = "Module 1: Strategic Vision & Team Alignment",
                    summary = "Learn how to communicate corporate vision effectively and align cross-functional engineering and business teams.",
                    videoDurationSeconds = 480,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    transcript = "Welcome to Module 1. In this lesson, we break down how leaders translate quarterly OKRs into tactical team sprints. Aligning engineering priorities with corporate vision prevents burnout and maximizes impact...",
                    keyTakeaways = "Clear vision reduces ambiguity by 40%\nAlign OKRs with weekly sprint goals\nFoster psychological safety in team standups"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m1_1Id,
                    questionText = "What is the primary benefit of aligning engineering sprint goals with corporate OKRs?",
                    optionA = "Eliminates all code reviews",
                    optionB = "Reduces ambiguity and aligns tactical execution with high-level business goals",
                    optionC = "Increases daily meeting durations",
                    optionD = "Replaces project management tools",
                    correctOptionIndex = 1,
                    explanation = "Aligning daily work with strategic objectives ensures high-impact focus and reduces wasteful misalignment."
                )
            )
            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m1_1Id,
                    questionText = "Which factor is critical for encouraging transparent feedback during team retrospectives?",
                    optionA = "Strict punitive policies",
                    optionB = "Psychological safety",
                    optionC = "Ignoring individual contributions",
                    optionD = "Bypassing manager 1-on-1s",
                    correctOptionIndex = 1,
                    explanation = "Psychological safety empowers team members to discuss challenges and admit mistakes without fear of retribution."
                )
            )

            val m1_2Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c1Id,
                    moduleOrder = 2,
                    title = "Module 2: Decision Making Under Uncertainty",
                    summary = "Frameworks for rapid evaluation, risk mitigation, and assertive decision-making in volatile market environments.",
                    videoDurationSeconds = 620,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    transcript = "Module 2 explores executive decision frameworks under incomplete information. We study the 70% rule: make decisions when you have 70% of the information you wish you had...",
                    keyTakeaways = "Use the 70% information threshold\nReversible vs Irreversible decisions (One-way vs Two-way doors)\nPost-decision retrospective reviews"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m1_2Id,
                    questionText = "According to Jeff Bezos' decision framework, what is a 'Two-Way Door' decision?",
                    optionA = "A permanent, irreversible company restructuring",
                    optionB = "A decision that can be easily reversed if outcomes are undesirable",
                    optionC = "A decision requiring 100% consensus",
                    optionD = "A security protocol for physical offices",
                    correctOptionIndex = 1,
                    explanation = "Two-way door decisions are reversible and should be made quickly without excessive bureaucracy."
                )
            )

            // --- COURSE 2: Soft Skills - Professional Communication ---
            val c2Id = courseDao.insertCourse(
                CourseEntity(
                    title = "High-Impact Technical Presentation & Public Speaking",
                    category = "SOFT_SKILLS",
                    description = "Structure technical ideas clearly for non-technical executives, master body language, and deliver persuasive project proposals.",
                    thumbnailKey = "communication",
                    level = "Beginner",
                    estimatedHours = 3,
                    instructorName = "Rachel Vance, M.A."
                )
            ).toInt()

            val m2_1Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c2Id,
                    moduleOrder = 1,
                    title = "Module 1: The Minto Pyramid Communication Principle",
                    summary = "Start with the conclusion first: how to structure email updates, architecture docs, and executive presentations.",
                    videoDurationSeconds = 510,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    transcript = "Executives are time-constrained. The Minto Pyramid Principle dictates giving the key recommendation or answer FIRST, followed by supporting logical categories...",
                    keyTakeaways = "Lead with the bottom-line recommendation (BLUF)\nGroup supporting points logically into 3 key pillars\nTailor depth based on audience technical expertise"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m2_1Id,
                    questionText = "What does 'BLUF' stand for in executive corporate communication?",
                    optionA = "Brief Line Under Formatting",
                    optionB = "Bottom Line Up Front",
                    optionC = "Business Logic Universal Flow",
                    optionD = "Basic Leadership User Feedback",
                    correctOptionIndex = 1,
                    explanation = "BLUF (Bottom Line Up Front) emphasizes presenting the key takeaway or conclusion first."
                )
            )

            // --- COURSE 3: Electronic Engineering - Analog Circuits ---
            val c3Id = courseDao.insertCourse(
                CourseEntity(
                    title = "Analog Circuit Analysis & Op-Amp Fundamentals",
                    category = "ELECTRONIC_ENGINEERING",
                    description = "Comprehensive study of nodal analysis, Kirchhoff's laws, operational amplifiers, active filters, and signal conditioning circuits.",
                    thumbnailKey = "circuits",
                    level = "Intermediate",
                    estimatedHours = 6,
                    instructorName = "Prof. Alan Turing, Ph.D."
                )
            ).toInt()

            val m3_1Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c3Id,
                    moduleOrder = 1,
                    title = "Module 1: Fundamental Nodal & Mesh Circuit Analysis",
                    summary = "Solve complex linear resistor, capacitor, and inductor networks using Kirchhoff's Current and Voltage Laws.",
                    videoDurationSeconds = 720,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    transcript = "Welcome to Analog Engineering. Today we cover Kirchhoff's Current Law (KCL) where algebraic sum of currents entering a node is zero. We apply nodal voltage equations to determine branch currents...",
                    keyTakeaways = "KCL: Sum of currents entering a node equals zero\nKVL: Sum of voltage drops around a closed loop is zero\nSuperposition theorem applies only to linear networks"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m3_1Id,
                    questionText = "According to Kirchhoff's Current Law (KCL), what is true for any circuit junction node?",
                    optionA = "The voltage at the node must be 0 Volts",
                    optionB = "The algebraic sum of currents entering the node is equal to zero",
                    optionC = "Resistance across the node approaches infinity",
                    optionD = "Power dissipated at the node is maximized",
                    correctOptionIndex = 1,
                    explanation = "KCL states that charge conservation dictates the total current entering a node equals total current leaving it."
                )
            )
            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m3_1Id,
                    questionText = "What is the equivalent resistance of two 100 Ohm resistors connected in parallel?",
                    optionA = "200 Ohms",
                    optionB = "100 Ohms",
                    optionC = "50 Ohms",
                    optionD = "25 Ohms",
                    correctOptionIndex = 2,
                    explanation = "For two identical parallel resistors R, Req = R / 2 = 100 / 2 = 50 Ohms."
                )
            )

            val m3_2Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c3Id,
                    moduleOrder = 2,
                    title = "Module 2: Operational Amplifiers & Feedback Configuration",
                    summary = "Analyze ideal Op-Amps, inverting vs non-inverting amplifiers, virtual ground concepts, and gain-bandwidth product.",
                    videoDurationSeconds = 840,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    transcript = "Operational amplifiers are fundamental building blocks. In an ideal Op-Amp with negative feedback, input currents are zero and non-inverting (+) and inverting (-) terminal voltages are virtually equal...",
                    keyTakeaways = "Virtual Short Concept: V+ equals V- in negative feedback\nInverting Gain = -Rf / Rin\nNon-Inverting Gain = 1 + (Rf / Rin)"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m3_2Id,
                    questionText = "In an ideal Op-Amp with negative feedback, what is the voltage difference between the inverting (-) and non-inverting (+) input pins?",
                    optionA = "Equal to the supply voltage Vcc",
                    optionB = "Zero Volts (Virtual Ground/Virtual Short)",
                    optionC = "Infinitely high voltage",
                    optionD = "Dependent on input frequency only",
                    correctOptionIndex = 1,
                    explanation = "Negative feedback drives the differential input voltage V+ - V- to zero."
                )
            )

            // --- COURSE 4: Electronic Engineering - Microcontrollers ---
            val c4Id = courseDao.insertCourse(
                CourseEntity(
                    title = "ARM Cortex-M Embedded Systems & Peripherals",
                    category = "ELECTRONIC_ENGINEERING",
                    description = "Learn 32-bit MCU architecture, memory mapping, interrupt vectors (NVIC), timers, PWM, UART, SPI, and I2C protocols.",
                    thumbnailKey = "microcontroller",
                    level = "Advanced",
                    estimatedHours = 8,
                    instructorName = "Eng. Hiroshi Tanaka"
                )
            ).toInt()

            val m4_1Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c4Id,
                    moduleOrder = 1,
                    title = "Module 1: ARM Cortex-M Memory Map & NVIC Interrupts",
                    summary = "Explore register structures, memory mapped I/O, stack pointer initialization, and low-latency interrupt handling.",
                    videoDurationSeconds = 900,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoylikes.mp4",
                    transcript = "ARM Cortex-M core utilizes a unified 4GB linear memory map. Peripherals are mapped directly into memory addresses. The Nested Vectored Interrupt Controller (NVIC) supports low latency preemptive interrupt handling...",
                    keyTakeaways = "Unified memory space for code, RAM, and memory-mapped peripherals\nNVIC supports nested interrupt priority levels\nHardware auto-stacking on interrupt entry"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m4_1Id,
                    questionText = "What is the primary role of the Nested Vectored Interrupt Controller (NVIC) in Cortex-M microcontrollers?",
                    optionA = "Generating PWM clock frequencies",
                    optionB = "Managing hardware interrupt priorities, nesting, and fast execution response",
                    optionC = "Converting analog signals to digital values",
                    optionD = "Managing wireless Bluetooth connectivity",
                    correctOptionIndex = 1,
                    explanation = "NVIC handles hardware interrupts with preemption, priority grouping, and minimal latency."
                )
            )

            // --- COURSE 5: Electronic Engineering - Digital Logic & FPGA ---
            val c5Id = courseDao.insertCourse(
                CourseEntity(
                    title = "Digital Logic Architecture & Verilog HDL Design",
                    category = "ELECTRONIC_ENGINEERING",
                    description = "Combinational logic synthesis, sequential state machines (Mealy/Moore), timing closure, and FPGA implementation.",
                    thumbnailKey = "digital_logic",
                    level = "Intermediate",
                    estimatedHours = 5,
                    instructorName = "Dr. Maya Lin"
                )
            ).toInt()

            val m5_1Id = moduleDao.insertModule(
                ModuleEntity(
                    courseId = c5Id,
                    moduleOrder = 1,
                    title = "Module 1: Sequential Logic & Flip-Flop Timing Analysis",
                    summary = "Understand D flip-flops, setup time, hold time, clock skew, and metastablity in digital integrated circuits.",
                    videoDurationSeconds = 660,
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    transcript = "Digital synchronous design relies on clean clocking. Setup time is the minimum time data must remain stable BEFORE the active clock edge, while hold time is required AFTER the clock edge...",
                    keyTakeaways = "Setup Time (t_su): Data stable before clock edge\nHold Time (t_h): Data stable after clock edge\nViolating timing causes metastability"
                )
            ).toInt()

            quizDao.insertQuestion(
                QuizQuestionEntity(
                    moduleId = m5_1Id,
                    questionText = "What happens when setup time or hold time specifications are violated in a D flip-flop?",
                    optionA = "The clock frequency automatically doubles",
                    optionB = "The output can enter an unstable state known as metastability",
                    optionC = "The circuit converts to an analog amplifier",
                    optionD = "The flip-flop fuses permanently",
                    correctOptionIndex = 1,
                    explanation = "Timing violations cause unpredictable intermediate voltages (metastability) before settling."
                )
            )
        }
    }
}
