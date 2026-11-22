GPU Utilization Management System
COSC230 OOP Project – Lab Section B4

Project Description:
Our project is a Java app that manages GPU usage for university students. Students can register, reserve GPUs through submitting jobs, check their time allowance,and track their job progress. Adminstrators can can approve or reject registrations, monitor GPU usage , and manage the students' jobs.
The system has 4 simulated GPUs and acts on a first come first serve (FCFS) basis.

Requirements:

Java JDK

VS Code with Java extensions

How to Run:
1. Download the project from GitHub.
2. Open the OOP-project-B4 folder in VS Code.
3. Make sure Java is installed.
4. Open project.java.
5. Click Run to start the application.
6. Log in as an administrator or register a student account.

How the system works:
Students first request an account and wait for the adminstrator to approve it. When the student is approved, they are allocated 100 GPU minutes automatically and can start submitting jobs.

There are small, medium, large, and very large jobs, each taking different amount of minutes. The system assigns these jobs to the 4 GPUs. If all are occupied, then the jobs wait in an ordered queue (FCFS).

Students can check their jobs, view predicted waiting times and can cancel jobs. Admins can monitor the system and manage student accounts. They can grant more GPU minutes to students and can advance the simulation clock by a minute each time.

Data Saving: 
The application saves student accounts, jobs, GPU info, allowance, simulation time in a file named "data.txt"

When the application is closed and opened again, the saved information is restored so users can conitnue from where they closed.

Project Files:

model: Contains students, administrators, jobs, GPUs, and time allowances.
logic: Contains the scheduler, simulation clock, and application state.
UI: Contains the login, registration, student, and administrator screens.
storage: Handles saving and loading data.
project.java: Starts the application.
Driver.java: Tests the main system functions.
StorageTest.java: Tests saving and loading.

Testing:
We did multiple tests. We tested console tests, storage tests, and GUI tests. 

OOP Concepts:

Our project uses the 5 required OOP concepts of encapsulation, inheritance, interfaces, polymorphism, and collections.
