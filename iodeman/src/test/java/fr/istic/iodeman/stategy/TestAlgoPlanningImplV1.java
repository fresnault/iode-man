package fr.istic.iodeman.stategy;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.istic.iodeman.model.OralDefense;
import fr.istic.iodeman.model.Participant;
import fr.istic.iodeman.model.Person;
import fr.istic.iodeman.model.Planning;
import fr.istic.iodeman.model.Priority;
import fr.istic.iodeman.model.Role;
import fr.istic.iodeman.model.Room;
import fr.istic.iodeman.model.TimeBox;
import fr.istic.iodeman.model.Unavailability;
import fr.istic.iodeman.strategy.AlgoPlanning;
import fr.istic.iodeman.strategy.AlgoPlanningImplV1;
import fr.istic.iodeman.utils.AlgoPlanningUtils;

public class AlgoPlanningImplV1Test {

	@Test
	public void test1() {
		
		List<Participant> participants = Lists.newArrayList();
		
		for(int i=1; i < 15; i++) {
			
			Person p1 = new Person();
			p1.setId(i);
			p1.setFirstName("Student "+i);
			p1.setRole(Role.STUDENT);
			
			Person p2 = new Person();
			p2.setId(i);
			p2.setFirstName("Prof "+i);
			p2.setRole(Role.PROF);		
			
			Participant participant = new Participant();
			participant.setStudent(p1);
			participant.setFollowingTeacher(p2);
			
			participants.add(participant);
			
		}
		
		Room room1 = new Room();
		room1.setName("i227");
		Room room2 = new Room();
		room2.setName("i58");
		
		Priority priority1 = new Priority();
		priority1.setRole(Role.STUDENT);
		priority1.setWeight(1);
		
		Priority priority2 = new Priority();
		priority2.setWeight(10);
		priority2.setRole(Role.PROF);
		
		Planning planning = new Planning();
		planning.setParticipants(participants);
		planning.setRooms(Lists.newArrayList(room1, room2));
		planning.setPriorities(Lists.newArrayList(priority1, priority2));
		
		List<TimeBox> timeBoxes = Lists.newArrayList();
		
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,13,8,0)).toDate(),
				(new DateTime(2015,1,13,9,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,13,9,0)).toDate(),
				(new DateTime(2015,1,13,10,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,13,10,0)).toDate(),
				(new DateTime(2015,1,13,11,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,13,11,0)).toDate(),
				(new DateTime(2015,1,13,12,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,14,8,0)).toDate(),
				(new DateTime(2015,1,14,9,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,14,9,0)).toDate(),
				(new DateTime(2015,1,14,10,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,14,10,0)).toDate(),
				(new DateTime(2015,1,14,11,0)).toDate()
		));
		timeBoxes.add(new TimeBox(
				(new DateTime(2015,1,14,11,0)).toDate(),
				(new DateTime(2015,1,14,12,0)).toDate()
		));
		
		List<Unavailability> unavailabilities = Lists.newArrayList();
		
		Unavailability ua1 = new Unavailability();
		ua1.setPerson(participants.get(0).getStudent());
		ua1.setPeriod(new TimeBox(
				(new DateTime(2015,1,13,8,0)).toDate(),
				(new DateTime(2015,1,13,12,0)).toDate()
		));
		unavailabilities.add(ua1);
		
		Unavailability ua2 = new Unavailability();
		ua2.setPerson(participants.get(1).getFollowingTeacher());
		ua2.setPeriod(new TimeBox(
				(new DateTime(2015,1,14,8,0)).toDate(),
				(new DateTime(2015,1,14,12,0)).toDate()
		));
		unavailabilities.add(ua2);
		
		Unavailability ua3 = new Unavailability();
		ua3.setPerson(participants.get(2).getFollowingTeacher());
		ua3.setPeriod(new TimeBox(
				(new DateTime(2015,1,13,10,0)).toDate(),
				(new DateTime(2015,1,13,12,0)).toDate()
		));
		unavailabilities.add(ua3);
		
		AlgoPlanning algo = new AlgoPlanningImplV1();
		algo.configure(planning);
		
		Collection<OralDefense> results = algo.execute(timeBoxes, unavailabilities);
		
		printResults(results);
		
		// verify that there is the same number of participants than generated oral defenses
		assertEquals(participants.size(), results.size());
		
		List<Participant> finalParticipants = Lists.newArrayList();
		Map<TimeBox, List<Room>> finalBoxes = Maps.newHashMap();
		
		for(OralDefense oralDefense : results) {
			
			// verify if a participant is not present twice
			Participant p = oralDefense.getComposition();
			assertFalse(finalParticipants.contains(p));
			finalParticipants.add(p);
			
			// verify that there is only one oral defense for a couple (TimeBox, Room)
			List<Room> allocatedRooms = finalBoxes.get(oralDefense.getTimebox());
			if (allocatedRooms != null) {
				assertFalse(allocatedRooms.contains(oralDefense.getRoom()));
				allocatedRooms.add(oralDefense.getRoom());
			}else{
				finalBoxes.put(oralDefense.getTimebox(), Lists.newArrayList(oralDefense.getRoom()));
			}

		}

		// verify that the given unavailabilities have been respected
		assertTrue(checkIfUnavailabilityRespected(results, ua1));
		assertTrue(checkIfUnavailabilityRespected(results, ua2));
		assertTrue(checkIfUnavailabilityRespected(results, ua3));
		
	}
	
	public void printResults(Collection<OralDefense> results) {
		
		for(OralDefense oralDefense : results) {
			
			DateTime date1 = new DateTime(oralDefense.getTimebox().getFrom());
			DateTime date2 = new DateTime(oralDefense.getTimebox().getTo());
			
			System.out.println(
					"oral defense for " +
					oralDefense.getComposition().getStudent().getFirstName()
					+ " - " + oralDefense.getComposition().getFollowingTeacher().getFirstName()
					+ " set on " + date1.toString("dd/MM/yyyy HH:mm")
					+ " - " + date2.toString("dd/MM/yyyy HH:mm")
					+ " in " + oralDefense.getRoom().getName()
		);
			
		}
		
	}
	
	public boolean checkIfUnavailabilityRespected(Collection<OralDefense> results, Unavailability ua) {
		
		for(OralDefense oralDefense : results) {
			
			if (ua.getPerson().equals(oralDefense.getComposition().getStudent())
					|| ua.getPerson().equals(oralDefense.getComposition().getFollowingTeacher())) {
				
				return AlgoPlanningUtils.isAvailable(ua, oralDefense.getTimebox());
				
			}
			
		}
		
		return true;
		
	}
	
}