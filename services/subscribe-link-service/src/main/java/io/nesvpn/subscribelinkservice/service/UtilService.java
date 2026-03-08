package io.nesvpn.subscribelinkservice.service;

import io.nesvpn.subscribelinkservice.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UtilService {

    public String createDescription(User user, String username) {
        StringBuilder description = new StringBuilder();
        if (user.getTgId() != null) {
            description.append(username);
        }
        if (user.getEmail() != null) {
            description.append(" ");
            description.append(user.getEmail());
        }
        return description.toString();
    }
}
