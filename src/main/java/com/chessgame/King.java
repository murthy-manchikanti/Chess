package com.chessgame;

public class King extends Piece {
    private boolean hasMoved = false;

    public King(PieceColor color, Position position) {
        super(color, position);
    }

    @Override
    public boolean isValidMove(Position newPosition, Piece[][] board) {
        int rowDiff = Math.abs(position.getRow() - newPosition.getRow());
        int colDiff = Math.abs(position.getColumn() - newPosition.getColumn());

        boolean isOneSquareMove = rowDiff <= 1 && colDiff <= 1 && !(rowDiff == 0 && colDiff == 0);
        if (isOneSquareMove) {
            Piece destinationPiece = board[newPosition.getRow()][newPosition.getColumn()];
            return destinationPiece == null || destinationPiece.getColor() != this.getColor();
        }

        // Castling Logic
        if (!hasMoved && rowDiff == 0 && colDiff == 2) {
            return canCastle(newPosition, board);
        }

        return false;
    }

    private boolean canCastle(Position newPosition, Piece[][] board) {
        int row = position.getRow();
        int col = position.getColumn();
        int newCol = newPosition.getColumn();
        boolean isKingside = newCol > col;
        int rookCol = isKingside ? 7 : 0;

        Piece rook = board[row][rookCol];
        if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;

        int step = isKingside ? 1 : -1;
        for (int i = col + step; i != rookCol; i += step) {
            if (board[row][i] != null) return false;
        }

        // Ensure the king does not move through check (simplified here)
        // Implement a proper `isSquareAttacked` check in ChessBoard
        return true;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public boolean hasMoved() {
        return hasMoved;
    }
}