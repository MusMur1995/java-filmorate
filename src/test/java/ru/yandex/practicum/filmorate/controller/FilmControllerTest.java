package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmControllerTest {

    @Autowired
    private FilmController filmController;
    private Film film;

    @BeforeEach
    void setUp() {
        film = new Film();
        film.setName("Film Name");
        film.setDescription("Film Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
    }

    @Test
    @DisplayName("Создание фильма с корректными данными")
    void createFilm_WithValidData_ShouldSuccess() {
        Film createdFilm = filmController.createFilm(film);

        assertNotNull(createdFilm.getId());
        assertEquals("Film Name", createdFilm.getName());
    }

    @Test
    @DisplayName("Создание фильма с пустым названием")
    void createFilm_WithEmptyName_ShouldThrowException() {
        film.setName("");

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с названием из пробелов")
    void createFilm_WithBlankName_ShouldThrowException() {
        film.setName("   ");

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с описанием длиннее 200 символов")
    void createFilm_WithTooLongDescription_ShouldThrowException() {
        film.setDescription("A".repeat(201));

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с описанием ровно 200 символов")
    void createFilm_WithDescriptionExactly200Chars_ShouldSuccess() {
        film.setDescription("A".repeat(200));

        Film createdFilm = filmController.createFilm(film);

        assertNotNull(createdFilm.getId());
        assertEquals(200, createdFilm.getDescription().length());
    }

    @Test
    @DisplayName("Создание фильма с датой релиза раньше 28.12.1895")
    void createFilm_WithTooEarlyReleaseDate_ShouldThrowException() {
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с датой релиза 28.12.1895")
    void createFilm_WithMinReleaseDate_ShouldSuccess() {
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Film createdFilm = filmController.createFilm(film);

        assertNotNull(createdFilm.getId());
        assertEquals(LocalDate.of(1895, 12, 28), createdFilm.getReleaseDate());
    }

    @Test
    @DisplayName("Создание фильма с отрицательной продолжительностью")
    void createFilm_WithNegativeDuration_ShouldThrowException() {
        film.setDuration(-1);

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с нулевой продолжительностью")
    void createFilm_WithZeroDuration_ShouldThrowException() {
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    @DisplayName("Создание фильма с null значениями")
    void createFilm_WithNullValues_ShouldThrowException() {
        Film nullFilm = new Film();  // отдельный объект для этого теста
        nullFilm.setName(null);
        nullFilm.setDescription(null);
        nullFilm.setReleaseDate(null);
        nullFilm.setDuration(null);

        assertThrows(ValidationException.class, () -> filmController.createFilm(nullFilm));
    }
}