/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.Model;

import java.sql.Date;

/**
 *
 * @author ACER
 */
public class Leave {

    private int leaveId;
    private int absentTeacherId;
    private Date leaveStartDate;
    private Date leaveEndDate;
    private String leaveReason;
    private String leaveNotes;
    private String leaveStatus;

    public int getLeaveID() {
        return leaveId;
    }

    public void setLeaveID(int leaveId) {
        this.absentTeacherId = leaveId;
    }

    public int getAbsentTeacherID() {
        return absentTeacherId;
    }

    public void setAbsentTeacherID(int absentTeacherId) {
        this.absentTeacherId = absentTeacherId;
    }

    public Date getLeaveStartDate() {
        return leaveStartDate;
    }

    public void setLeaveStartDate(Date leaveStartDate) {
        this.leaveStartDate = leaveStartDate;
    }

    public Date getLeaveEndDate() {
        return leaveEndDate;
    }

    public void setLeaveEndDate(Date leaveEndDate) {
        this.leaveEndDate = leaveEndDate;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }

    public String getLeaveNotes() {
        return leaveNotes;
    }

    public void setLeaveNotes(String leaveNotes) {
        this.leaveNotes = leaveNotes;
    }

    public String getLeaveStatus() {
        return leaveStatus;
    }

    public void setLeaveStatus(String leaveStatus) {
        this.leaveStatus = leaveStatus;
    }
}