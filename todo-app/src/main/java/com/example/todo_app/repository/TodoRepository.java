package com.example.todo_app.repository;

//package com.example.todoapp.repository;

import com.example.todo_app.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

}
