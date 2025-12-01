package com.chessgame;

import javax.swing.*;

public class ChessBoard {
    private Piece[][] board;

    public ChessBoard() {
        this.board = new Piece[8][8];
        setupPieces();
    }

    public Piece[][] getBoard() {
        return board;
    }

    public Piece getPiece(int row, int column) {
        return board[row][column];
    }

    public void setPiece(int row, int column, Piece piece) {
        board[row][column] = piece;
        if (piece != null) {
            piece.setPosition(new Position(row, column));
        }
    }

    private void setupPieces() {
        int[][] rookPositions = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};
        int[][] knightPositions = {{0, 1}, {0, 6}, {7, 1}, {7, 6}};
        int[][] bishopPositions = {{0, 2}, {0, 5}, {7, 2}, {7, 5}};

        for (int[] pos : rookPositions) 
            board[pos[0]][pos[1]] = new Rook(getColor(pos[0]), new Position(pos[0], pos[1]));

        for (int[] pos : knightPositions) 
            board[pos[0]][pos[1]] = new Knight(getColor(pos[0]), new Position(pos[0], pos[1]));

        for (int[] pos : bishopPositions) 
            board[pos[0]][pos[1]] = new Bishop(getColor(pos[0]), new Position(pos[0], pos[1]));

        board[0][3] = new Queen(PieceColor.BLACK, new Position(0, 3));
        board[7][3] = new Queen(PieceColor.WHITE, new Position(7, 3));
        
        board[0][4] = new King(PieceColor.BLACK, new Position(0, 4));
        board[7][4] = new King(PieceColor.WHITE, new Position(7, 4));

        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(PieceColor.BLACK, new Position(1, i));
            board[6][i] = new Pawn(PieceColor.WHITE, new Position(6, i));
        }
    }

    private PieceColor getColor(int row) {
        return (row == 0 || row == 1) ? PieceColor.BLACK : PieceColor.WHITE;
    }

    public void movePiece(Position start, Position end, boolean isEnPassantMove) {
        Piece movingPiece = board[start.getRow()][start.getColumn()];
        if (movingPiece == null) return;
        if (!isEnPassantMove && !movingPiece.isValidMove(end, board)) return;

        // Handle Castling
        if (movingPiece instanceof King && Math.abs(start.getColumn() - end.getColumn()) == 2) {
            handleCastling((King) movingPiece, start, end);
        }

        if (movingPiece instanceof Pawn) {
            // Handle pawn promotion
            if ((movingPiece.getColor() == PieceColor.WHITE && end.getRow() == 0) ||
                (movingPiece.getColor() == PieceColor.BLACK && end.getRow() == 7)) {
                promotePawn((Pawn) movingPiece, end);
                board[start.getRow()][start.getColumn()] = null;
                return;
            }
        }

        // Move the piece
        board[end.getRow()][end.getColumn()] = movingPiece;
        if (movingPiece instanceof Pawn) {
            ((Pawn) movingPiece).move(end);
        } else {
            movingPiece.setPosition(end);
        }
        board[start.getRow()][start.getColumn()] = null;

        // Update moved status
        if (movingPiece instanceof King) {
            ((King) movingPiece).setHasMoved(true);
        } else if (movingPiece instanceof Rook) {
            ((Rook) movingPiece).setHasMoved(true);
        }
    }

    private void handleCastling(King king, Position start, Position end) {
        int row = start.getRow();
        int col = start.getColumn();
        int newCol = end.getColumn();
        boolean isKingside = newCol > col;
        int rookCol = isKingside ? 7 : 0;
        int newRookCol = isKingside ? 5 : 3;

        Piece rook = board[row][rookCol];
        if (rook instanceof Rook && !((Rook) rook).hasMoved()) {
            board[row][newRookCol] = rook;
            rook.setPosition(new Position(row, newRookCol));
            board[row][rookCol] = null;
        }
    }

    private void promotePawn(Pawn pawn, Position position) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        String selectedOption = (String) JOptionPane.showInputDialog(null,
                "Choose a piece to promote your pawn to:",
                "Pawn Promotion",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (selectedOption != null) {
            Piece promotedPiece = null;
            switch (selectedOption) {
                case "Queen":
                    promotedPiece = new Queen(pawn.getColor(), position);
                    break;
                case "Rook":
                    promotedPiece = new Rook(pawn.getColor(), position);
                    break;
                case "Bishop":
                    promotedPiece = new Bishop(pawn.getColor(), position);
                    break;
                case "Knight":
                    promotedPiece = new Knight(pawn.getColor(), position);
                    break;
            } 
            board[position.getRow()][position.getColumn()] = promotedPiece;
        }
    }
}