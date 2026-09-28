package com.karnofleet.repository;

import com.karnofleet.entity.LinehaulSchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LinehaulScheduleRepo {
    int insert(LinehaulSchedule schedule);

    Optional<LinehaulSchedule> findById(@Param("id") UUID id);

    Optional<LinehaulSchedule> findByScheduleCode(@Param("scheduleCode") String scheduleCode);

    List<LinehaulSchedule> findAll();

    List<LinehaulSchedule> findByStatus(@Param("status") String status);

    List<LinehaulSchedule> findActiveSchedules();
}
