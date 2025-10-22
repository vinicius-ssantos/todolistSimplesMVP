package com.viniss.todo.it

import com.viniss.todo.auth.AppUserEntity
import com.viniss.todo.auth.AppUserRepository
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.viniss.todo.config.TestMockMvcConfig::class)
abstract class OwnerIsolationIT {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var appUsers: AppUserRepository
    @Autowired protected lateinit var lists: com.viniss.todo.repo.TodoListRepository
    @Autowired protected lateinit var tasks: com.viniss.todo.repo.TaskRepository
    lateinit var ownerId: UUID
    lateinit var intruderId: UUID
    lateinit var ownersListId: UUID
    lateinit var ownersTaskId: UUID

    @BeforeEach
    fun setup() {
        tasks.deleteAll()
        lists.deleteAll()

        ownerId = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
        intruderId = UUID.fromString("00000000-0000-0000-0000-0000000000b2")

        // Dono existe porque há FK de user_id -> app_user(id)
        appUsers.save(AppUserEntity(id = ownerId, email = "owner@example.com", passwordHash = "noop"))
        appUsers.save(AppUserEntity(id = intruderId, email = "intruder@example.com", passwordHash = "noop"))

        val list = lists.save(TodoListEntity(name = "Do Owner").apply { userId = ownerId })
        ownersListId = list.id

        val task = tasks.save(TaskEntity(list = list, title = "Privada", position = 0).apply { userId = ownerId })
        ownersTaskId = task.id
    }

    // ---------- READ ----------
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2") // intruso
    fun `GET list de outro usuario - 404`() {
        mockMvc.get("$LISTS_BASE_PATH/$ownersListId").andExpect { status { isNotFound() } }
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `GET task de outro usuario - 404`() {
        mockMvc.get("$LISTS_BASE_PATH/$ownersListId/tasks/$ownersTaskId").andExpect { status { isNotFound() } }
    }

    // ---------- UPDATE ----------
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `PATCH list de outro usuario - 404`() {
        mockMvc.patch("$LISTS_BASE_PATH/$ownersListId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"X"}"""
        }.andExpect { status { isNotFound() } }
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `PATCH task de outro usuario - 404`() {
        mockMvc.patch("$LISTS_BASE_PATH/$ownersListId/tasks/$ownersTaskId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"X"}"""
        }.andExpect { status { isNotFound() } }
    }

    // ---------- CREATE (em lista de outro) ----------
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `POST task em list de outro usuario - 404 da lista`() {
        mockMvc.post("$LISTS_BASE_PATH/$ownersListId/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"nao deveria criar"}"""
        }.andExpect { status { isNotFound() } }
    }

    // ---------- DELETE ----------
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `DELETE list de outro usuario -  404`() {
        mockMvc.delete("$LISTS_BASE_PATH/$ownersListId").andExpect { status { isNotFound() } }
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000b2")
    fun `DELETE task de outro usuario -  404`() {
        mockMvc.delete("$LISTS_BASE_PATH/$ownersListId/tasks/$ownersTaskId").andExpect { status { isNotFound() } }
    }

    // Sanity check: o dono consegue acessar normalmente
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000a1")
    fun `owner ainda acessa`() {
        val before = tasks.count()
        mockMvc.delete("$LISTS_BASE_PATH/$ownersListId/tasks/$ownersTaskId").andExpect { status { isNoContent() } }
        assertThat(tasks.count()).isEqualTo(before - 1)
    }
}
