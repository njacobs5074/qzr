package com.sctrcd.qzr.web.resources;

import com.sctrcd.qzr.facts.Question;
import com.sctrcd.qzr.web.controllers.HrMaxQuizController;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Injected into the {@link HrMaxQuizController} as a tool for building {@link QuestionResource}.
 *
 * @author Stephen Masters
 */
@Component
public class QuestionResourceAssembler extends ResourceAssemblerSupport<Question, QuestionResource> {

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    AnswerResourceAssembler answerResourceAssembler;

    public QuestionResourceAssembler() {
        super(HrMaxQuizController.class, QuestionResource.class);
    }

    @Override
    public QuestionResource toResource(Question question) {

        System.out.println("Question: " + question);

        QuestionResource resource = createResourceWithId("questions/" + question.getKey(), question);

        resource.setKey(question.getKey());
        resource.setText(question.getQuestion());
        resource.setAnswerType(question.getAnswerType());

        resource.addOptions(question.getOptions());

        if (question.getAnswer() != null) {
            resource.setAnswer(answerResourceAssembler.toResource(question.getAnswer()));
        }

        try {
            resource.add(linkTo(methodOn(HrMaxQuizController.class).answer(question.getKey(), new AnswerResource()))
                                 .withRel("answer"));
            resource.add(linkTo(methodOn(HrMaxQuizController.class).skip(question.getKey()))
                                 .withRel("skip"));
        }
        catch (BadRequestException e) {
            logger.warn("Failed to add links", e);
        }

        return resource;
    }

}
