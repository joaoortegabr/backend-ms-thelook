package com.thelook.ms_social.services;

import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.entities.CreatorNode;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreatorNodeService {

    private final CreatorNodeRepository creatorNodeRepository;

    public CreatorNodeService(CreatorNodeRepository creatorNodeRepository) {
        this.creatorNodeRepository = creatorNodeRepository;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void save(Creator creator) {
        creatorNodeRepository.save(new CreatorNode(creator.getId(), creator.getName()));
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void delete(UUID creatorId) {

        if (!creatorNodeRepository.existsById(creatorId))
            throw new ResourceNotFoundException("Creator not found");

        creatorNodeRepository.deepDeleteCreator(creatorId);
    }
}
