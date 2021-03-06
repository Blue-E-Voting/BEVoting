package blueEVoting;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

//import javax.swing.*;

/*View control acts as the median between the User->display->ballot. takes the interaction and gives results to transfer*/

/**
 * A big area for improvement here is to define many of the action listeners out right,
 * so that they aren't made so many times.
 */
public class ViewController {
	
	private int currentView;
	private int currentState;
	private Candidate[] candidates;
	private Candidate selectedCandidate;
	private Voter localVoter;
	private Display display;
	private DatabaseController db;
	private Ballot ballot;
	
	public static void main(String args[]) {
		DatabaseController db = new DatabaseController();
		ViewController view = new ViewController();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.startView();
			}
		});
	}
	
	public void startView() {
		currentView = 0;
		display = new Display();
		db = new DatabaseController();
		ballot = new Ballot(db.getNumberOfPositions());
		display.start();
		display.displayVoterValidation(new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				if ( Integer.parseInt( display.getTextFieldText() ) == 12347 ) displayAdminPanel();
				else if ( validateVoter( Integer.parseInt( display.getTextFieldText() ) ) == true ){
					ballot.setVoterID(Integer.parseInt( display.getTextFieldText() ));
					moveToNextView();
				}
				else display.warn("Incorrect Registration Number.");
			} 
		});
	}
	
	public void restartView() {
		ballot = new Ballot(db.getNumberOfPositions());
		currentView = 0;
		display.stopTimer();
		display.restart();
		display.displayVoterValidation(new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				if ( Integer.parseInt( display.getTextFieldText() ) == 12347 ) displayAdminPanel();
				else if ( display.getTextFieldText().matches("[0-9]+") && validateVoter( Integer.parseInt( display.getTextFieldText() ) ) == true ) {
					ballot.setVoterID(Integer.parseInt( display.getTextFieldText() ));
					moveToNextView();
				}
				else display.warn("Incorrect Registration Number.");
			} 
		});
	}
	
	/*verifies admin - Not in first iteration*/
	private boolean validateAdmin() {
		return db.validateAdmin();
	};
	
	/*verifies voter*/
	private boolean validateVoter(int id) {
		return db.validateVoter(id);
	};
	
	/*process that shows user next selection after finishing a previous selection*/
	private void moveToNextView() {
		// Positions can be displayed by just moving to each next view.
		// Check if out of positions, if so, move to final commit view.
		display.setUpTimer(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	restartView();
            	display.warn("You have been signed out due to inactivity.");
            }
        });
		
		if ( db.getNumberOfPositions() > currentView) {
			displayCandidateView(currentView);
			currentView++;
		} else if (db.getNumberOfPositions() == currentView) {
			// Display final verification
			displayVerification();
			currentView++;
		} else {
			// reset
			restartView();
		}
	};
	
	private void displayVerification() {
		display.displayVerification(ballot.getSelections(), new ActionListener() {
			public void actionPerformed( ActionEvent event) {
				ballot.submit();
				System.out.println("Ballot submitted");
				db.submitBallot(ballot);
				//db.countResults();     //TO TEST COUNT RESULTS CURRENTLY
				//if(db.recountCertification() == true) System.out.println("it's true");
				//else System.out.println("false");//to test recountCert
				moveToNextView();
			}
		}, new ActionListener() {
			public void actionPerformed( ActionEvent event) {
				currentView = 0;
				System.out.println("Ballot being redone");
				moveToNextView();
			}
		});
	}
	private void displayCandidateView(int position) {
		display.displayCandidateView(db.getCandidates(position), new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				// Submit candidate selection
				if (display.getSelectedCandidate() == null) display.warn("Please select a Candidate first.");
				else {
					ballot.setCandidate(display.getSelectedCandidate(), position);
					moveToNextView();
				}
			} 
		});
	}
	
	private void displayAdminPanel() {
		display.displayAdminPanel(new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				Candidate candidate = new Candidate();
				candidate.setCandidateName(display.getTextFieldText() );
				candidate.setCandidatePosition((String) display.getComboSelection());
				db.storeCandidate(candidate);
				db.showCandidates();
			} 
		}, new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				restartView();
			}
		}, new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				displayCountView();
			}
		});
	}
	
	private void displayCountView() {
		
		display.displayCountView(db.countResults(), db.recountCertification(), new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				displayAdminPanel();
			}
		});
		if (!db.recountCertification()) {
			display.warn("Ballots do not match Number of Voters.");
		}
	}
	

}
