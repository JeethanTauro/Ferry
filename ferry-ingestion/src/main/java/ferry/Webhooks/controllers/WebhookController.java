package ferry.Webhooks.controllers;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

//so the full endpoint is going to be http://ferry.com/webhooks/{id}
@RequestMapping("/webhooks")
public class WebhookController {
    //1) a webhook event occurs
    //2) we need to first authenticate it using secret key in the db
    //3) once authenticated, we have to check for which consumer is this event sent using the id

    @PostMapping("/{id}")
    public void createWebhooks(){

    }
}
