import java.io.IOException; // Handles errors realted to reading and writing files
import java.nio.file.Files; //Provides methods to create, read, and write files
import java.nio.file.Path; // Represents the location of a file or folder
import java.time.LocalDateTime; //Represents date and time

import logic.AppState; // Stores the shared application data
import model.AccessRequest; //Represents a student's registration request
import storage.DataStore; // handles saving and loading application data

/* This class test what happens when saving fails 
    after the application data has already changed in memory */
public class SaveFailureTest{
    //Main method: starting point of the test program
    public static void main(String args[]) throws Exception {
        //Create a temporary folder for testing
        //This prevents the test from changing the real data.txt
        Path directory = Files.createTempDirectory("gpu-save-failure-test-");

        //Create a DataStore object that saves inside
        //the temprary folder instead of the project folder
        DataStore store = new DataStore(directory.resolve("data.txt"));


        //Create a new empty application state
        AppState state = new AppState();

       // Initialize the simulation clock
       // The starting time is 21 November 2026 at 10:00
       state.initializeClock(LocalDateTime.of(2026, 11, 21, 10, 0));
        //Save the initial application state
        //At this point, no student registration has been added
        store.save(state);

        //Print a message confirming the first save succeded
        System.out.println("TEST 1: Initial state saved sucessfully.");

        //Create a temporary student registration request
        //This request is only used for testing
        AccessRequest request = new AccessRequest(9999, "Save Failure Test", "test@example.com", "0501234567", "test123");

        //Add the reistration request to the application memory
        state.addRequest(request);

        //Print a mesage for confirming the request was added
        System.out.println("TEST 2: Request added to memory.");

        //We now want to simulate a save failure
        //Instead of damaging the real save file
        //we create a destination that cannot be used correctly

        //Create a path called "blocked" inside the temporary folder
        Path blockingFile = directory.resolve("blocked");

        //Create a regular file named "blocked"
        //This will important b/c next step will incorrectly try to use it as a folder
        Files.writeString(blockingFile, "This is a file, not a directory");
        
        //Create another DataStore object
        //Its save path is blocked/data.txt
        //Since "blocked" is a file and not a folder
        // saving to this location should fail
        DataStore failingStore = new DataStore(blockingFile.resolve("data.txt"));

        //We use true or false to indicate whether saving failed 
        // It starts as false b/c no failure has occured yet
        boolean saveFailed = false;

        try {

            //Attempt to save the modified application state
            //This should throw an IOException
            //b/c the destination folder is invalid
            failingStore.save(state);
        } catch (IOException ex) {

            //This block runs if saving fails 
            //Records that the expected error occured
            saveFailed = true;

            // Print a message confirming the failure was detected
            System.out.println("TEST 3: Save failure detected.");
        }
        //Check whether the registration request still exists in the application memory
        //getRequests() return the registration requests
        //stream() allows us to examine those requests
        //anyMatch() checks whether at least one request has ID 9999
        boolean requestInMemory = state.getRequests().stream().anyMatch(r -> r.getId() == 9999);

        //Load the last successfully saved application state
        //This reads the original temporary data.txt
        //which was saved before adding the new regtration

        AppState savedState = store.load();

        //Check whether registration 999 exits in the saved file
        //Since the new registration was never saved successfully,
        //it should not appear in the loded state
        boolean requestOnDisk = savedState.getRequests().stream().anyMatch(r -> r.getId() == 9999);

        //Print a heading for the test results 
        System.out.println();
        System.out.println("=== SAVE FAILURE RESULTS ===");

        //Displays whether the save operation failed
        System.out.println("Save failed: " + saveFailed);

        //Display whther the registration remains in memory 
        System.out.println("Request in memory: " + requestInMemory);

        //Display whther the registration remains in saved file 
        System.out.println("Request in saved file: " + requestOnDisk);

        //Check whether the expected inconsistency occured
        //All 3 conditions must be true:
        // 1. The save operation failed
        // 2. The request remains in memory 
        // 3. The request doesn't exist in the saved file
        if (saveFailed && requestInMemory && !requestOnDisk) {
            //Print a warning that the inconsistency was reported
            System.out.println();
            System.out.println("CONSISTENCY RISK CONFIRMED:");
        //  Eplains the problem
        System.out.println("The registration remains in memory after saving fails.");
        } else {
            //This message appears if the expected inconsistency wasn't observed
            System.out.println("The expected inconsistency wasn't observed.");
        }
        //Delete the temporary blocking file, the test directory and its other files remian available
        Files.deleteIfExists(blockingFile);

        //Print an empty line to seperate the results
        System.out.println();
        //Display the temporary directory location
        //This helps locate the test files if needed
        System.out.println("Test directory: " + directory);
        }
     
    }

