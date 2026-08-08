package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val usersList by viewModel.allUsers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var showUserDialog by remember { mutableStateOf<UserEntity?>(null) } // null = closed, UserEntity(id=0) = new, UserEntity(id>0) = edit
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filteredUsers = remember(usersList, searchQuery, selectedRoleFilter) {
        usersList.filter { u ->
            val matchesRole = when (selectedRoleFilter) {
                "ADMIN" -> u.role == "ADMIN"
                "EMPLOYEE" -> u.role == "EMPLOYEE"
                "ENGINEER_STUDENT" -> u.role == "ENGINEER_STUDENT"
                else -> true
            }
            val matchesSearch = u.fullName.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.department.contains(searchQuery, ignoreCase = true)
            matchesRole && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Account CRUD Management", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_users_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showUserDialog = UserEntity(
                        id = 0,
                        email = "",
                        passwordHash = "temp123",
                        fullName = "",
                        role = "EMPLOYEE",
                        department = "Engineering",
                        designation = "Corporate Trainee",
                        avatarInitials = "CU"
                    )
                },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add User", style = MaterialTheme.typography.labelLarge) },
                containerColor = ExecutiveBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("admin_add_user_fab")
            )
        },
        containerColor = BackgroundWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            statusMessage?.let { msg ->
                Surface(
                    color = PassGreenBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PassGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PassGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search users by name, email, department...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_bar"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Role Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedRoleFilter == "ALL",
                    onClick = { selectedRoleFilter = "ALL" },
                    label = { Text("All (${usersList.size})") }
                )
                FilterChip(
                    selected = selectedRoleFilter == "EMPLOYEE",
                    onClick = { selectedRoleFilter = "EMPLOYEE" },
                    label = { Text("Employees") }
                )
                FilterChip(
                    selected = selectedRoleFilter == "ENGINEER_STUDENT",
                    onClick = { selectedRoleFilter = "ENGINEER_STUDENT" },
                    label = { Text("Engineers") }
                )
                FilterChip(
                    selected = selectedRoleFilter == "ADMIN",
                    onClick = { selectedRoleFilter = "ADMIN" },
                    label = { Text("Admins") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredUsers, key = { it.id }) { u ->
                    UserAccountRowCard(
                        user = u,
                        onEdit = { showUserDialog = u },
                        onDelete = { userToDelete = u },
                        onToggleActive = {
                            val updated = u.copy(isActive = !u.isActive)
                            viewModel.adminUpdateUser(updated) {
                                statusMessage = "Updated status for ${u.fullName}"
                            }
                        }
                    )
                }
            }
        }
    }

    // Add / Edit User Dialog (Create/Update CRUD)
    showUserDialog?.let { editingUser ->
        UserEditDialog(
            user = editingUser,
            onDismiss = { showUserDialog = null },
            onSave = { updatedUser ->
                if (updatedUser.id == 0) {
                    viewModel.adminCreateUser(updatedUser) { success, err ->
                        if (success) {
                            statusMessage = "Created user account: ${updatedUser.fullName}"
                            showUserDialog = null
                        } else {
                            statusMessage = "Failed: $err"
                        }
                    }
                } else {
                    viewModel.adminUpdateUser(updatedUser) {
                        statusMessage = "Updated user account: ${updatedUser.fullName}"
                        showUserDialog = null
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog (Delete CRUD)
    userToDelete?.let { deletingUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User Account?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete user account '${deletingUser.fullName}' (${deletingUser.email})?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminDeleteUser(deletingUser.id) {
                            statusMessage = "Deleted user ${deletingUser.fullName}"
                            userToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_user_button")
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserAccountRowCard(
    user: UserEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_row_${user.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ExecutiveBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.avatarInitials,
                    style = MaterialTheme.typography.titleMedium,
                    color = ExecutiveBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RoleBadge(role = user.role)
                }

                Text(
                    text = "${user.email} • ${user.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.scale(0.8f)
                )

                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_user_button_${user.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = ExecutiveBlue, modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_user_button_${user.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = ErrorRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf(user.fullName) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf(user.passwordHash) }
    var role by remember { mutableStateOf(user.role) }
    var department by remember { mutableStateOf(user.department) }
    var designation by remember { mutableStateOf(user.designation) }

    var roleExpanded by remember { mutableStateOf(false) }

    val isNew = user.id == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Create User Account" else "Edit User Details",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("user_dialog_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Corporate Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("user_dialog_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("user_dialog_password_input")
                )

                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("User Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("EMPLOYEE") }, onClick = { role = "EMPLOYEE"; roleExpanded = false })
                        DropdownMenuItem(text = { Text("ENGINEER_STUDENT") }, onClick = { role = "ENGINEER_STUDENT"; roleExpanded = false })
                        DropdownMenuItem(text = { Text("ADMIN") }, onClick = { role = "ADMIN"; roleExpanded = false })
                    }
                }

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifEmpty { "CU" }
                    val updated = user.copy(
                        fullName = name,
                        email = email,
                        passwordHash = password,
                        role = role,
                        department = department,
                        designation = designation,
                        avatarInitials = initials
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExecutiveBlue),
                modifier = Modifier.testTag("save_user_dialog_button")
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
