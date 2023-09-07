package com.example.wayz.Repository;


import com.example.wayz.Model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ReportRepository extends JpaRepository<Report,Integer> {
Report findReportById(Integer id);



}
