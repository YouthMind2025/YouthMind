package ai.youthmind.access;

import org.springframework.stereotype.Service;

@Service
public class DataSharingController {
    public boolean canShareWith(String target) { return target != null && !target.isBlank(); }
}


