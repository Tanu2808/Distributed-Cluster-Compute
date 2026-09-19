package com.cluster.worker.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cluster.worker.service.TaskService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TaskService taskService;

    @Test
    public void testGetActiveTasks() throws Exception {
        when(taskService.getActiveTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/tasks/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isEmpty());
    }
}
