/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package com.Controller;

import com.Dao.ScheduleDao;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.Dao.SubstitutionAssignmentDao;
import com.Dao.SubstitutionDao;
import com.Dao.TeacherDao;
import com.Model.SubstitutionAssignments;
import com.Model.Teacher;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import util.Database;

@WebServlet(name = "SubstitutionAssignmentServlet", urlPatterns = {"/SubstitutionAssignmentServlet"})
public class SubstitutionAssignmentServlet extends HttpServlet {
    // Telegram Bot settings

    private static final String TELEGRAM_BOT_TOKEN = "7875648625:AAHuc62etZLbotxvdUwvfKT9qCH1fBfGfk8";
    String CHAT_ID = null;  // Chat ID for the teacher(teacher need to get their user id first using the IDBOT from telegram)
    private SubstitutionAssignmentDao SubstitutionAssignmentDao;

    public void init() {
        SubstitutionAssignmentDao = new SubstitutionAssignmentDao();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        System.out.println("Action received: " + action); // Debugging output

        try {
            if (action == null) {
                response.sendRedirect("SUBSTITUTIONS.jsp");
                return;
            }

            switch (action) {
                case "update":
                    updateSubstitutionAssignment(request, response);
                    break;
                case "modify":
                    modifySubstitutionAssignment(request, response);
                    break;
                case "confirm":
                    confirmSubstitutionAssignment(request, response);
                    break;
                case "delete":
                    deleteSubstitutionAssignment(request, response);
                    break;
                case "delete2":
                    deleteSubstitutionAssignment2(request, response);
                    break;
                default:
                    listSubstitutionAssignment(request, response);
                    break;
            }
        } catch (SQLException | IOException | ServletException ex) {
            ex.printStackTrace();
        }

    }

    private void listSubstitutionAssignment(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException, ServletException {
        List<SubstitutionAssignments> listAssgn = SubstitutionAssignmentDao.displayAllSubstitutionAssignment();
        request.setAttribute("listAssgn", listAssgn);
        RequestDispatcher dispatcher = request.getRequestDispatcher("substitutionAssignmentHistory.jsp");
        dispatcher.forward(request, response);
    }

    private void updateSubstitutionAssignment(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException, ServletException {
        PrintWriter out = response.getWriter();

        try {
            String[] substitutionIds = request.getParameterValues("substitutionId");
            String[] scheduleIds = request.getParameterValues("scheduleId");
            String[] substituteTeacherIds = request.getParameterValues("substituteTeacherId");
            String[] remarks1 = request.getParameterValues("remarks");
            String[] status1 = request.getParameterValues("status");

            boolean allSuccess = true;

            Map<Date, Set<Integer>> teacherAssignments = new HashMap<>(); // Key: Date, Value: Set of periods assigned

            for (int i = 0; i < substitutionIds.length; i++) {
                int substitutionId = Integer.parseInt(substitutionIds[i]);
                int scheduleId = Integer.parseInt(scheduleIds[i]);
                int substituteTeacherId = Integer.parseInt(substituteTeacherIds[i]);
                String remarks = remarks1[i];
                String status = status1[i];

                int period = ScheduleDao.getPeriodByScheduleId(scheduleId);
                Date substitutionDate = SubstitutionDao.getSubstitutionDateById(substitutionId);
                String absentTeacherName = TeacherDao.getTeacherNameById(SubstitutionDao.getAbsentTeacherBySubstitutionId(substitutionId));

                // Check if assignment is made
                if (substituteTeacherId == 0 && (remarks == null || remarks.trim().isEmpty())) {
                    out.print("<script>");
                    out.print("alert(\"Period " + period + " for teacher " + absentTeacherName
                            + " is not yet assigned for substitution. Please revise your substitution assignment.\");");
                    out.print("window.history.back();");
                    out.print("</script>");
                    return;  // Stop execution 
                }

                // Check for conflicting substitution assignment
                if (substituteTeacherId != 0 && (remarks.equalsIgnoreCase("Split Class")
                        || remarks.equalsIgnoreCase("Cancelled")
                        || remarks.equalsIgnoreCase("Event"))) {

                    out.print("<script>");
                    out.print("alert(\"Conflicting substitution assignment detected!Please revise your substitution at period " + period + " for teacher " + absentTeacherName + ".\");");
                    out.print("window.history.back();");
                    out.print("</script>");
                    return;  // Stop execution 
                }
                boolean conflictExists = false;

                // only check conflicts for valid teachers
                if (substituteTeacherId != 0) {
                    // Get assigned periods for the teacher(only period with duplicated teacher will be saved)
                    if (!teacherAssignments.containsKey(substitutionDate)) {
                        teacherAssignments.put(substitutionDate, SubstitutionAssignmentDao.getAssignedPeriodsForTeacher(substituteTeacherId, substitutionDate));
                    }

                    Set<Integer> assignedPeriods = teacherAssignments.get(substitutionDate);

                    // Check if teacher is already assigned to the same period
                    if (assignedPeriods != null && assignedPeriods.contains(period)) {
                        if (!remarks.equalsIgnoreCase("Combine class")) {
                            String conflictTeacherName = TeacherDao.getTeacherNameById(substituteTeacherId);
                            out.print("<script>alert(\"Teacher " + conflictTeacherName + " is already assigned for period " + period + ". "
                                    + "Please change another teacher or change the remarks to 'Combine class'\"); window.history.back();</script>");
                            return;  // Stop execution if conflict is found
                        }
                        conflictExists = true;
                    }

                    // No conflict, add the period to prevent future duplicates
                    if (!conflictExists) {
                        assignedPeriods.add(period);
                    }
                }
                SubstitutionAssignments newAssgn = new SubstitutionAssignments(substitutionId, scheduleId, substituteTeacherId, remarks, status);

                int success = SubstitutionAssignmentDao.update(newAssgn);
                if (success <= 0) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                out.print("<script>alert('Substitution Assignments updated successfully!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
            } else {
                out.print("<script>alert('Failed to update some assignments. Please try again!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("<script>alert('An error occurred while updating. Please try again!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
        }
    }

    private void modifySubstitutionAssignment(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException, ServletException {
        PrintWriter out = response.getWriter();

        try {
            String[] substitutionIds = request.getParameterValues("substitutionId");
            String[] scheduleIds = request.getParameterValues("scheduleId");
            String[] substituteTeacherIds = request.getParameterValues("substituteTeacherId");
            String[] remarks1 = request.getParameterValues("remarks");
            String[] status1 = request.getParameterValues("status");

            boolean allSuccess = true;

            Map<Date, Set<Integer>> teacherAssignments = new HashMap<>(); // Key: Date, Value: Set of periods assigned

            for (int i = 0; i < substitutionIds.length; i++) {
                int substitutionId = Integer.parseInt(substitutionIds[i]);
                int scheduleId = Integer.parseInt(scheduleIds[i]);
                int substituteTeacherId = Integer.parseInt(substituteTeacherIds[i]);
                String remarks = remarks1[i];
                String status = status1[i];

                int period = ScheduleDao.getPeriodByScheduleId(scheduleId);
                String absentTeacherName = TeacherDao.getTeacherNameById(SubstitutionDao.getAbsentTeacherBySubstitutionId(substitutionId));
                Date substitutionDate = SubstitutionDao.getSubstitutionDateById(substitutionId);

                // Check if assignment is made
                if (substituteTeacherId == 0 && (remarks == null || remarks.trim().isEmpty())) {
                    out.print("<script>");
                    out.print("alert(\"Period " + period + " on " + substitutionDate + " for teacher " + absentTeacherName
                            + " is not yet assigned for substitution. Please revise your substitution assignment.\");");
                    out.print("window.history.back();");
                    out.print("</script>");
                    return;  // Stop execution 
                }

                // Check for conflicting substitution assignment
                if (substituteTeacherId != 0 && (remarks.equalsIgnoreCase("Split Class")
                        || remarks.equalsIgnoreCase("Cancelled")
                        || remarks.equalsIgnoreCase("Event"))) {

                    out.print("<script>");
                    out.print("alert(\"Conflicting substitution assignment detected!Please revise your substitution at period " + period + " on " + substitutionDate
                            + " for teacher " + absentTeacherName + ".\");");
                    out.print("window.history.back();");
                    out.print("</script>");
                    return;  // Stop execution 
                }

                boolean conflictExists = false;

                // only check conflicts for valid teachers
                if (substituteTeacherId != 0) {
                    // Get assigned periods for the teacher(only period with duplicated teacher will be saved)
                    if (!teacherAssignments.containsKey(substitutionDate)) {
                        teacherAssignments.put(substitutionDate, SubstitutionAssignmentDao.getAssignedPeriodsForTeacher(substituteTeacherId, substitutionDate));
                    }

                    Set<Integer> assignedPeriods = teacherAssignments.get(substitutionDate);

                    // Check if teacher is already assigned to the same period
                    if (assignedPeriods != null && assignedPeriods.contains(period)) {
                        if (!remarks.equalsIgnoreCase("Combine class")) {
                            String conflictTeacherName = TeacherDao.getTeacherNameById(substituteTeacherId);
                            out.print("<script>alert(\"Teacher " + conflictTeacherName + " is already assigned for period " + period + " on " + substitutionDate + ". "
                                    + "Please change another teacher or change the remarks to 'Combine class'\"); window.history.back();</script>");
                            return;  // Stop execution if conflict is found
                        }
                        conflictExists = true;
                    }

                    // No conflict, add the period to prevent future duplicates
                    if (!conflictExists) {
                        assignedPeriods.add(period);
                    }
                }
                SubstitutionAssignments newAssgn = new SubstitutionAssignments(substitutionId, scheduleId, substituteTeacherId, remarks, status);

                int success = SubstitutionAssignmentDao.update(newAssgn);
                if (success <= 0) {
                    allSuccess = false;
                }
            }

            if (allSuccess) {
                out.print("<script>alert('Substitution Assignments updated successfully!'); window.location.href='manageAssignments.jsp';</script>");
            } else {
                out.print("<script>alert('Failed to update some assignments. Please try again!'); window.location.href='manageAssignments.jsp';</script>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("<script>alert('An error occurred while updating. Please try again!'); window.location.href='manageAssignments.jsp';</script>");
        }
    }

    private void confirmSubstitutionAssignment(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException, ServletException {
        PrintWriter out = response.getWriter();

        try {
            String[] substitutionIds = request.getParameterValues("substitutionId");
            String[] scheduleIds = request.getParameterValues("scheduleId");
            String[] substituteTeacherIds = request.getParameterValues("substituteTeacherId");
            String[] remarks1 = request.getParameterValues("remarks");
            String[] status1 = request.getParameterValues("status");

            boolean allSuccess = true;
            List<SubstitutionAssignments> confirmedAssignments = new ArrayList<>();

            for (int i = 0; i < substitutionIds.length; i++) {
                int substitutionId = Integer.parseInt(substitutionIds[i]);
                int scheduleId = Integer.parseInt(scheduleIds[i]);
                int substituteTeacherId = Integer.parseInt(substituteTeacherIds[i]);
                String remarks = remarks1[i];
                String status = status1[i];

                SubstitutionAssignments newAssgn = new SubstitutionAssignments(substitutionId, scheduleId, substituteTeacherId, remarks, status);
                int success = SubstitutionAssignmentDao.confirm(newAssgn);
                if (success <= 0) {
                    allSuccess = false;
                } else {
                    confirmedAssignments.add(newAssgn);
                }
            }

            if (allSuccess) {
                for (SubstitutionAssignments assignment : confirmedAssignments) {
                    sendSubstitutionNotification(assignment);
                }
                out.print("<script>alert('Substitution Assignments confirmed successfully!'); window.location.href='SUBSTITUTION.jsp';</script>");
            } else {
                out.print("<script>alert('Failed to confirm some assignments. Please try again!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("<script>alert('An error occurred while confirming. Please try again!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
        }
    }

    private void deleteSubstitutionAssignment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            int substitutionId = Integer.parseInt(request.getParameter("substitutionId"));
            int scheduleId = Integer.parseInt(request.getParameter("scheduleId"));
            int status = SubstitutionAssignmentDao.delete(substitutionId, scheduleId);
            if (status > 0) {
                response.getWriter().print("<script>alert('Substitution Assignment record deleted successfully!');</script>");
                request.getRequestDispatcher("SUBSTITUTIONS.jsp").include(request, response);
            } else {
                response.getWriter().print("<script>alert('Sorry! Unable to delete substitution assignment record.');</script>");
                request.getRequestDispatcher("SUBSTITUTIONS.jsp").include(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("<script>alert('An error occurred while deleting the record. Please try again!'); window.location.href='SUBSTITUTIONS.jsp';</script>");
        }
    }

    private void deleteSubstitutionAssignment2(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            int substitutionId = Integer.parseInt(request.getParameter("substitutionId"));
            int scheduleId = Integer.parseInt(request.getParameter("scheduleId"));
            int status = SubstitutionAssignmentDao.delete(substitutionId, scheduleId);
            if (status > 0) {
                response.getWriter().print("<script>alert('Substitution Assignment record deleted successfully!');</script>");
                request.getRequestDispatcher("manageAssignments.jsp").include(request, response);
            } else {
                response.getWriter().print("<script>alert('Sorry! Unable to delete substitution assignment record.');</script>");
                request.getRequestDispatcher("manageAssignments.jsp").include(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("<script>alert('An error occurred while deleting the record. Please try again!'); window.location.href='manageAssignments.jsp';</script>");
        }
    }

    //methods
    private void sendSubstitutionNotification(SubstitutionAssignments assignment) {

        int substituteTeacherId = assignment.getSubstituteTeacherID();
        Teacher substituteTeacher = TeacherDao.getTeacherById(substituteTeacherId);
        String telegramId = substituteTeacher.getTelegramId();
        try {
            //skip if the teacher does not have a telegram chat id
            if (telegramId == null || telegramId.trim().isEmpty()) {
                return;
            }
            CHAT_ID = telegramId;
            SubstitutionAssignments assgn = SubstitutionAssignmentDao.getSubstitutionAssigmentDetailsBySubstitutionIdAndScheduleId(assignment.getSubstitutionId(), assignment.getScheduleId());
            String message = "Dear " + TeacherDao.getTeacherNameById(assgn.getSubstituteTeacherID()) + ",\n\n"
                    + "You have been assigned to substitute for today's class (" + assgn.getScheduleDay() + ", " + assgn.getSubstitutionDate() + ").\n\n"
                    + "Absent Teacher Name: " + TeacherDao.getTeacherNameById(assgn.getAbsentTeacherId()) + "\n"
                    + "Period: " + assgn.getPeriod() + "\n"
                    + "Subject: " + assgn.getSubjectName() + "\n"
                    + "Class: " + assgn.getClassName() + "\n"
                    + "Remarks: " + (assgn.getRemarks() == null ? "" : assgn.getRemarks()) + "\n\n"
                    + "Best regards,\nAssistant Principal";

            // URL-encode the message to ensure no illegal characters
            String encodedMessage = URLEncoder.encode(message, "UTF-8");

            // Construct the Telegram URL
            String urlString = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMessage;
            System.out.println("URL: " + urlString); // This will show the full URL in the console

            // Create a URL object and open a connection
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            // Send the request
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Success
                System.out.println("Telegram notification sent successfully.");
            } else {
                // Handle error response
                System.out.println("Failed to send Telegram notification. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error sending Telegram notification: " + e.getMessage());
        }
    }
}
