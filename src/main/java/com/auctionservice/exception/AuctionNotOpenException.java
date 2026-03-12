package com.auctionservice.exception;

public class AuctionNotOpenException extends RuntimeException {

    public AuctionNotOpenException(String message) {
        super(message);
    }
}
