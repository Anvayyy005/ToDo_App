package com.example.todo_app.controller;

import com.example.todo_app.model.Todo;
import com.example.todo_app.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TodoController {

    private List<SseEmitter> emitters = new ArrayList<>();  // Keeps track of all connected clients

    @Autowired
    private TodoService todoService;

    @GetMapping("/")
    public String viewHomePage(Model model, HttpSession session) {
        // Retrieve the message and success flag from the session
        String message = (String) session.getAttribute("message");
        Boolean success = (Boolean) session.getAttribute("success");

        // Add it to the model and then remove it from the session
        model.addAttribute("message", message);
        model.addAttribute("success", success);
        session.removeAttribute("message");
        session.removeAttribute("success");

        // Display the list of tasks
        model.addAttribute("listTodos", todoService.getAllTodos());
        return "index";
    }

    @GetMapping("/showNewTodoForm")
    public String showNewTodoForm(Model model) {
        Todo todo = new Todo();
        model.addAttribute("todo", todo);
        return "new_todo";
    }

    @PostMapping("/saveTodo")
    public String saveTodo(@ModelAttribute("todo") Todo todo, HttpSession session) {
        boolean isNew = (todo.getId() == null);

        if (!isNew) {
            // If ID exists, it's an update
            Todo existingTodo = todoService.getTodoById(todo.getId());
            if (existingTodo != null) {
                existingTodo.setTask(todo.getTask());
                existingTodo.setDueDate(todo.getDueDate());
                existingTodo.setCompleted(todo.isCompleted());
                todoService.saveTodo(existingTodo);
                session.setAttribute("message", "Task updated!");
                session.setAttribute("success", true);
                broadcastUpdate("Task updated: " + existingTodo.getTask());
            }
        } else {
            // If ID doesn't exist, it's a new task
            todoService.saveTodo(todo);
            session.setAttribute("message", "New task added");
            session.setAttribute("success", true);
            broadcastUpdate("New task added: " + todo.getTask());
        }

        return "redirect:/";  // Redirect to prevent message duplication on refresh
    }

    @PostMapping("/completeTodo/{id}")
    public String completeTodo(@PathVariable(value = "id") Long id, HttpSession session) {
        Todo existingTodo = todoService.getTodoById(id);
        if (existingTodo != null && !existingTodo.isCompleted()) {
            existingTodo.setCompleted(true);
            todoService.saveTodo(existingTodo);
            session.setAttribute("message", "Task has been completed!");
            session.setAttribute("success", true);
            broadcastUpdate("Task completed: " + existingTodo.getTask());
        }
        return "redirect:/";
    }

    @PostMapping("/deleteTodo/{id}")
    public String deleteTodo(@PathVariable(value = "id") Long id, HttpSession session) {
        try {
            todoService.deleteTodoById(id);
            session.setAttribute("message", "Task deleted successfully!");
            session.setAttribute("success", true);
            broadcastUpdate("Task deleted.");
        } catch (Exception e) {
            session.setAttribute("message", "Some error occurred. Could not delete task");
            session.setAttribute("success", false);
        }
        return "redirect:/";
    }

    @GetMapping("/editTodo/{id}")
    public String editTodoForm(@PathVariable("id") Long id, Model model) {
        Todo todo = todoService.getTodoById(id);
        if (todo != null) {
            model.addAttribute("todo", todo);
            return "new_todo";  // Reuse new_todo.html for editing the task
        } else {
            return "redirect:/";  // Redirect back if task not found
        }
    }

    // SSE endpoint to subscribe to real-time updates
    @GetMapping(value = "/todo/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUpdates() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);

        System.out.println("New client connected for updates.");  // Log the new connection

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }


    // Broadcast updates to all connected clients via SSE
    public void broadcastUpdate(String message) {
        System.out.println("Broadcasting update: " + message);
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("update").data(message));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);  // Clean up dead connections
        System.out.println("Update sent to clients.");
    }
}
