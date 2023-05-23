package com.hotel.Continental.model.dao;

import com.hotel.Continental.model.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface HabitacionDao extends JpaRepository<Habitacion, Integer>  {
    @Query("SELECT h FROM Habitacion h WHERE h.idHabitacion NOT IN (SELECT r.habitacion FROM Reserva r WHERE (r.fechaInicio <= ?1 AND r.fechaFin >= ?2) OR r.fechaInicio BETWEEN ?1 AND ?2 OR r.fechaFin BETWEEN ?1 AND ?2)")
    List<Habitacion> findHabitacionesLibres(Date fechaInicio, Date fechaFin);
}
