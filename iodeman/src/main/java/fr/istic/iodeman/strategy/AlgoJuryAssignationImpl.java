package fr.istic.iodeman.strategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.istic.iodeman.model.OralDefense;
import fr.istic.iodeman.model.Person;
import fr.istic.iodeman.model.Unavailability;
import fr.istic.iodeman.utils.AlgoPlanningUtils;

public class AlgoJuryAssignationImpl implements AlgoJuryAssignation {

	private Collection<OralDefense> oralDefenses;
	private Collection<Unavailability> unavailabilities;
	
	private Map<Person, List<OralDefense>> followings;
	private Map<Person, List<OralDefense>> assignations;
	
	private Collection<AssignationCandidate> candidates;
	
	private boolean hasNewAssignation = false;
	
	private class AssignationCandidate {
		
		private OralDefense oralDefense;
		private List<Person> possibleJurys;
		
		public AssignationCandidate(OralDefense oralDefense) {
			this.oralDefense = oralDefense;
			this.possibleJurys = Lists.newArrayList();
		}
		
		public OralDefense getOralDefense() {
			return oralDefense;
		}
		
		public List<Person> getPossibleJurys() {
			return possibleJurys;
		}
		
	}
	
	public void configure(Collection<OralDefense> oralDefenses, Collection<Unavailability> unavailabilities) {
		
		this.oralDefenses = Lists.newArrayList();
		this.unavailabilities = Lists.newArrayList();
		this.followings = Maps.newHashMap();
		this.assignations = Maps.newHashMap();
		this.candidates = Lists.newArrayList();
		
		if (oralDefenses != null) {
			this.oralDefenses.addAll(oralDefenses);
		}
		
		if (unavailabilities != null) {
			this.unavailabilities.addAll(unavailabilities);
		}
		
		// Collections initialisations
		for(OralDefense od : oralDefenses) {
			
			Person p = od.getComposition().getFollowingTeacher();
			List<OralDefense> fl = followings.get(p);
			if (fl == null) {
				fl = Lists.newArrayList();
				followings.put(p, fl);
			}
			fl.add(od);
			
			candidates.add(new AssignationCandidate(od));
			
		}
		
	}

	public Collection<OralDefense> execute() {
		
		Validate.notNull(oralDefenses);
		
		while (!candidates.isEmpty()) {
			
			hasNewAssignation = true;
			
			while(hasNewAssignation) {
			
				hasNewAssignation = false;
				
				for(AssignationCandidate candidat : Lists.newArrayList(candidates)) {
					
					System.out.println("try allocation for the oral defense of "
							+candidat.getOralDefense().getComposition().getStudent().getFirstName());
					tryAllocation(candidat, false);
					
				}
			
			}
			
			if (!candidates.isEmpty()) {
				forceAllocation();
			}
			
		}
		
		return oralDefenses;
		
	}
	
	private void tryAllocation(final AssignationCandidate candidate, final boolean withUnavailabilities) {
		
		Collection<Person> possibilities = Collections2.filter(followings.keySet(), new Predicate<Person>() {
			
			public boolean apply(final Person jury) {
				
				Integer nbAssignations = 0;
				List<OralDefense> juryAssignations = assignations.get(jury);
				if (juryAssignations != null) {
					nbAssignations = juryAssignations.size();
					// check if the jury is not assignated to another oral defense on this timebox
					for(OralDefense assignated : juryAssignations) {
						if (candidate.getOralDefense().getTimebox().equals(assignated.getTimebox())) {
							return false;
						}
					}
				}
				
				List<OralDefense> juryFollowings = followings.get(jury);
				Integer nbFollowings = juryFollowings.size();
				// check if the jury is not present at another oral defense as follower on this timebox
				for(OralDefense followed : juryFollowings) {
					if (candidate.getOralDefense().getTimebox().equals(followed.getTimebox())) {
						return false;
					}
				}
				
				if (withUnavailabilities) {
					
					Collection<Unavailability> uaJury = Collections2.filter(unavailabilities, new Predicate<Unavailability>() {
						public boolean apply(Unavailability ua) {
							return ua.getPerson().equals(jury);
						}
					});

					if (!AlgoPlanningUtils.isAvailable(uaJury, candidate.getOralDefense().getTimebox())) {
						return false;
					}
					
				}
				
				return (!jury.equals(candidate.getOralDefense().getComposition().getFollowingTeacher())
						&& nbAssignations < nbFollowings);
			}
		});
		
		System.out.println(possibilities.size()+" possibilities found!");
		
		candidate.getPossibleJurys().clear();
		candidate.getPossibleJurys().addAll(possibilities);
		
		if (candidate.getPossibleJurys().size() == 1) {
			
			assignJury(candidate, candidate.getPossibleJurys().get(0));
		
		}else if (!withUnavailabilities && !candidate.getPossibleJurys().isEmpty()) {
			
			tryAllocation(candidate, true);
		
		}
		
	}
	
	private void forceAllocation() {
		
		System.out.println("forcing allocation...");
		
		int nb = -1;
		AssignationCandidate selected = null;
		
		for(AssignationCandidate candidate : candidates) {
			if (nb == -1 || (candidate.getPossibleJurys().size() < nb && nb > 0)) {
				selected = candidate;
			}
		}
		
		if (selected != null) {
			assignJury(selected, selected.getPossibleJurys().get(0));
		}
		
	}
	
	private void assignJury(AssignationCandidate candidate, Person jury) {
		
		System.out.println("assignation of the jury "
				+jury.getFirstName()
				+" to the oral defense of "
				+candidate.getOralDefense().getComposition().getStudent().getFirstName());
		
		
		OralDefense od = candidate.getOralDefense();
		
		od.setJury(Lists.newArrayList(jury));
		
		List<OralDefense> juryAssignations = assignations.get(jury);
		if (juryAssignations == null) {
			juryAssignations = Lists.newArrayList();
			assignations.put(jury, juryAssignations);
		}
		juryAssignations.add(od);
		System.out.println("nb assignations for this jury: "+juryAssignations.size());
		
		this.candidates.remove(candidate);
		
		hasNewAssignation = true;
		
		System.out.println("oral defenses not assigned: "+candidates.size());
		
	}

}