package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.user.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {

    @Autowired
    private UserController userController;

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("validlogin");
        user.setName("User Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    @DisplayName("Создание пользователя с корректными данными")
    void createUser_WithValidData_ShouldSuccess() {
        User user = createTestUser();

        User createdUser = userController.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
    }

    @Test
    @DisplayName("Создание пользователя с пустым email")
    void createUser_WithEmptyEmail_ShouldThrowException() {
        User user = createTestUser();

        user.setEmail("");

        assertThrows(ConstraintViolationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с email без символа @")
    void createUser_WithEmailWithoutAtSymbol_ShouldThrowException() {
        User user = createTestUser();

        user.setEmail("invalid-email");

        assertThrows(ConstraintViolationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с логином содержащим пробелы")
    void createUser_WithLoginContainingSpaces_ShouldThrowException() {
        User user = createTestUser();

        user.setLogin("login with spaces");

        assertThrows(ConstraintViolationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Создание пользователя с пустым именем - должно использоваться имя из логина")
    void createUser_WithEmptyName_ShouldUseLoginAsName() {
        User user = createTestUser();

        user.setName("");

        User createdUser = userController.create(user);

        assertEquals("validlogin", createdUser.getName());
    }

    @Test
    @DisplayName("Создание пользователя с датой рождения в будущем")
    void createUser_WithFutureBirthday_ShouldThrowException() {
        User user = createTestUser();

        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ConstraintViolationException.class, () -> userController.create(user));
    }

    @Test
    @DisplayName("Обновление пользователя с корректными данными")
    void updateUser_WithValidData_ShouldSuccess() {
        User user = createTestUser();

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

        assertThrows(ConstraintViolationException.class, () -> userController.updateUser(updateData));
    }
}