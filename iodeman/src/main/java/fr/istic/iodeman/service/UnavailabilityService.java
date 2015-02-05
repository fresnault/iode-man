package fr.istic.iodeman.service;

import java.util.Collection;
import java.util.List;

import fr.istic.iodeman.dto.AgendaDTO;
import fr.istic.iodeman.model.Person;
import fr.istic.iodeman.model.Planning;
import fr.istic.iodeman.model.TimeBox;
import fr.istic.iodeman.model.Unavailability;

public interface UnavailabilityService {

	public List<Unavailability> findById(Integer id, String uid);

	public Unavailability create(Integer id, String uidperson, TimeBox period);

	public Unavailability delete(Integer id);
	
	public Collection<AgendaDTO> exportAgenda(Planning planning, Person person);

}
