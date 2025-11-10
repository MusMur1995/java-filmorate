package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    @Autowired
    private UserController userController;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setName("User Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("Создание пользователя с корректными данными")
    void createUser_WithValidData_ShouldSuccess() {
        User createdUser = userController.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
    }

    @Test
    @DisplayName("Создание пользователя с пустым email")
    void createUser_WithEmptyEmail_ShouldThrowException() {
        user.setEmail("");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с email без символа @")
    void createUser_WithEmailWithoutAtSymbol_ShouldThrowException() {
        user.setEmail("invalid-email");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с логином содержащим пробелы")
    void createUser_WithLoginContainingSpaces_ShouldThrowException() {
        user.setLogin("login with spaces");

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с пустым именем - должно использоваться имя из логина")
    void createUser_WithEmptyName_ShouldUseLoginAsName() {
        user.setName("");

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    @DisplayName("Создание пользователя с датой рождения в будущем")
    void createUser_WithFutureBirthday_ShouldThrowException() {
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Обновление пользователя с корректными данными")
    void updateUser_WithValidData_ShouldSuccess() {
        User createdUser = userController.create(user);

        User updateData = new User();
        updateData.setId(createdUser.getId());
        updateData.setEmail("updated@example.com");
        updateData.setLogin("updatedlogin");

        User updatedUser = userController.updateUser(updateData);

        assertEquals("updated@example.com", updatedUser.getEmail());
        assertEquals("updatedlogin", updatedUser.getLogin());
    }

    @Test
    @DisplayName("Обновление пользователя без указания ID")
    void updateUser_WithoutId_ShouldThrowException() {
        User updateData = new User();
        updateData.setEmail("updated@example.com");

        assertThrows(ValidationException.class, () -> userController.updateUser(updateData));
    }
}