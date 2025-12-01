package com.chessgame;

public class Pawn extends Piece {
    private boolean justMovedTwoSquares;

    public Pawn(PieceColor color, Position position) {
        super(color, position);
        this.justMovedTwoSquares = false;
    }

    public boolean hasJustMovedTwoSquares() {
        return justMovedTwoSquares;
    }

    public void setJustMovedTwoSquares(boolean value) {
        this.justMovedTwoSquares = value;
    }

    @Override
    public boolean isValidMove(Position newPosition, Piece[][] board) {
        int forwardDirection = color == PieceColor.WHITE ? -1 : 1;
        int rowDiff = (newPosition.getRow() - position.getRow()) * forwardDirection;
        int colDiff = newPosition.getColumn() - position.getColumn();

        // Regular forward move
        if (colDiff == 0 && rowDiff == 1 && board[newPosition.getRow()][newPosition.getColumn()] == null) {
            return true;
        }

        // Double move from starting position
        boolean isStartingPosition = (color == PieceColor.WHITE && position.getRow() == 6) ||
                (color == PieceColor.BLACK && position.getRow() == 1);
        if (colDiff == 0 && rowDiff == 2 && isStartingPosition
                && board[newPosition.getRow()][newPosition.getColumn()] == null) {
            int middleRow = position.getRow() + forwardDirection;
            if (board[middleRow][position.getColumn()] == null) {
                return true;
            }
        }

        // Capturing diagonally
        if (Math.abs(colDiff) == 1 && rowDiff == 1 && board[newPosition.getRow()][newPosition.getColumn()] != null &&
                board[newPosition.getRow()][newPosition.getColumn()].color != this.color) {
            return true;
        }

        return false;
    }
    
    public void move(Position newPosition) {
        justMovedTwoSquares = Math.abs(newPosition.getRow() - position.getRow()) == 2;
        this.position = newPosition;
    }
}