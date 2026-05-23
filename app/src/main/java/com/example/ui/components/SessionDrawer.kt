package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SessionEntity

@Composable
fun SessionDrawer(
    drawerState: DrawerState,
    sessions: List<SessionEntity>,
    currentSessionId: Int?,
    onSessionSelected: (Int) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phiên làm việc", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onNewSession) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo mới")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = session.id == currentSessionId,
                            onClick = { onSessionSelected(session.id) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            badge = {
                                IconButton(onClick = { onDeleteSession(session.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        },
        content = content
    )
}
