package com.karnofleet.dto.response;

import java.util.List;

public record HereMatrixResponse(
        String matrixId,
        Matrix matrix
) {

    public record Matrix(
            int numOrigins,
            int numDestinations,
            List<Long> travelTimes,
            List<Integer> distances,
            List<Integer> errorCodes
    ) {
    }
}
