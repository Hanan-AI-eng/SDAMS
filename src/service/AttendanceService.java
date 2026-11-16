package service;

import java.io.*;
import java.util.ArrayList;
import model.AttendanceRecord;
public class AttendanceService {
	private final String fileName ="data/attendance.txt";
	
	public void addAttendance(AttendanceRecord record) throws IOException{
		try (BufferedWriter writer =
				new BufferedWriter(new FileWriter(fileName, true))){
			
			writer.write(record.toFileString());
			writer.newLine();
		}
	}
	
	public ArrayList<AttendanceRecord> getAllAttendance()
	throws IOException{
		
		ArrayList<AttendanceRecord> records = new ArrayList<>();
		File file = new File(fileName);
		
		if (!file.exists()) {
			return records;
		}
		try (BufferedReader reader =
				new BufferedReader(new FileReader(file))){
			String line;
			
			while((line =reader.readLine())!= null) {
				
				String[] data = line.split("\\|", -1);
				
				if (data.length==5) {
					AttendanceRecord record = new AttendanceRecord(
							data[0],data[1],data[2],data[3], data[4]);
					records.add(record);
				}
			}
		}
		return records;
		
	}
	public ArrayList<AttendanceRecord> searchByStudentId(String studentId)
	        throws IOException {

	    ArrayList<AttendanceRecord> results = new ArrayList<>();

	    if (studentId == null || studentId.trim().isEmpty()) {
	        return results;
	    }

	    for (AttendanceRecord record : getAllAttendance()) {
	        if (record.getStudentId().equalsIgnoreCase(studentId.trim())) {
	            results.add(record);
	        }
	    }

	    return results;
	}
	public ArrayList<AttendanceRecord> searchByClassId(String classId)
	        throws IOException {

	    ArrayList<AttendanceRecord> results = new ArrayList<>();

	    if (classId == null || classId.trim().isEmpty()) {
	        return results;
	    }

	    for (AttendanceRecord record : getAllAttendance()) {
	        if (record.getClassId().equalsIgnoreCase(classId.trim())) {
	            results.add(record);
	        }
	    }

	    return results;
	}

	public ArrayList<AttendanceRecord> searchByDate(String date)
	        throws IOException {

	    ArrayList<AttendanceRecord> results = new ArrayList<>();

	    if (date == null || date.trim().isEmpty()) {
	        return results;
	    }

	    for (AttendanceRecord record : getAllAttendance()) {
	        if (record.getDate().equals(date.trim())) {
	            results.add(record);
	        }
	    }

	    return results;
	}
	public boolean updateAttendanceStatus(String attendanceId, String newStatus)
	        throws IOException {

	    if (attendanceId == null || attendanceId.trim().isEmpty()) {
	        return false;
	    }

	    ArrayList<AttendanceRecord> records = getAllAttendance();
	    boolean found = false;

	    for (AttendanceRecord record : records) {
	        if (record.getAttendanceId()
	                .equalsIgnoreCase(attendanceId.trim())) {

	            record.setStatus(newStatus);
	            found = true;
	            break;
	        }
	    }

	    if (found) {
	        try (BufferedWriter writer =
	                     new BufferedWriter(new FileWriter(fileName))) {

	            for (AttendanceRecord record : records) {
	                writer.write(record.toFileString());
	                writer.newLine();
	            }
	        }
	    }

	    return found;
	}
}

