package com.chessgame;

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ChessGame {
    private ChessBoard board;
    private boolean whiteTurn = true;
    private final List<String> moveHistory = new ArrayList<>();
    private Process stockfishProcess;
    private BufferedReader stockfishInput;
    private PrintWriter stockfishOutput;
    private boolean isStockfishInitialized = false;

    public ChessGame() {
        this.board = new ChessBoard();
        initializeStockfish();
    }

    private void initializeStockfish() {
        try {
            String resourcePath = "/stockfish/stockfish-macos";
            File tempStockfish = File.createTempFile("stockfish", null);
            tempStockfish.deleteOnExit();
    
            try (
                InputStream is = getClass().getResourceAsStream(resourcePath);
                FileOutputStream os = new FileOutputStream(tempStockfish)
            ) {
                if (is == null) {
                    throw new FileNotFoundException("Could not find Stockfish binary in resources: " + resourcePath);
                }
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
    
            if (!tempStockfish.setExecutable(true)) {
                throw new IOException("Failed to make Stockfish executable");
            }
    
            ProcessBuilder pb = new ProcessBuilder(tempStockfish.getAbsolutePath());
            stockfishProcess = pb.start();
            stockfishInput = new BufferedReader(new InputStreamReader(stockfishProcess.getInputStream()));
            stockfishOutput = new PrintWriter(new OutputStreamWriter(stockfishProcess.getOutputStream()), true);
    
            stockfishOutput.println("uci");
            waitForLine("uciok");
    
            stockfishOutput.println("isready");
            waitForLine("readyok");
    
            isStockfishInitialized = true;
            setStockfishSkillLevel(10);
    
        } catch (Exception e) {
            System.err.println("Failed to initialize Stockfish:");
            e.printStackTrace();
            stockfishOutput = null;
            stockfishInput = null;
            stockfishProcess = null;
            isStockfishInitialized = false;
        }
    }
    
    private void waitForLine(String expected) throws IOException {
        String line;
        while ((line = stockfishInput.readLine()) != null) {
            if (line.trim().equals(expected)) break;
        }
    }

    public void setStockfishSkillLevel(int level) {
        if (stockfishOutput != null && isStockfishInitialized) {
            if (level < 0 || level > 20) {
                System.err.println("Invalid Stockfish skill level: " + level + ". Must be between 0 and 20.");
                return;
            }
            stockfishOutput.println("setoption name Skill Level value " + level);
            stockfishOutput.println("setoption name UCI_LimitStrength value true");
            stockfishOutput.println("isready");
            try {
                String line;
                while ((line = stockfishInput.readLine()) != null) {
                    if (line.equals("readyok")) break;
                }
            } catch (Exception e) {
                System.err.println("Error confirming Stockfish readiness after setting skill level: " + e.getMessage());
            }
        } else {
            System.err.println("Stockfish is not initialized. Cannot set skill level.");
        }
    }

    public void closeStockfish() {
        if (stockfishOutput != null) {
            stockfishOutput.println("quit");
        }
        if (stockfishProcess != null) {
            try {
                stockfishProcess.destroy();
            } catch (Exception e) {
                System.err.println("Error closing Stockfish: " + e.getMessage());
            }
        }
    }

    public ChessBoard getBoard() {
        return this.board;
    }

    public void resetGame() {
        this.board = new ChessBoard();
        this.whiteTurn = true;
        moveHistory.clear();

        if (stockfishOutput != null) {
            stockfishOutput.println("ucinewgame");
            stockfishOutput.println("position startpos");
        } else {
            System.err.println("Stockfish output is not initialized. Cannot reset Stockfish.");
        }
    }

    public PieceColor getCurrentPlayerColor() {
        return whiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
    }

    private Position selectedPosition;

    public boolean isPieceSelected() {
        return selectedPosition != null;
    }

    public boolean handleSquareSelection(int row, int col) {
        if (selectedPosition == null) {
            Piece selectedPiece = board.getPiece(row, col);
            if (selectedPiece != null
                    && selectedPiece.getColor() == (whiteTurn ? PieceColor.WHITE : PieceColor.BLACK)) {
                selectedPosition = new Position(row, col);
                return false;
            }
        } else {
            boolean moveMade = makeMove(selectedPosition, new Position(row, col));
            selectedPosition = null;
            return moveMade;
        }
        return false;
    }

    public boolean isEnPassantMove(Position start, Position end, Piece movingPiece) {
        if (movingPiece instanceof Pawn) {
            int direction = movingPiece.getColor() == PieceColor.WHITE ? -1 : 1;
            int colDiff = Math.abs(start.getColumn() - end.getColumn());
            int rowDiff = (end.getRow() - start.getRow()) * direction;

            if (rowDiff == 1 && colDiff == 1) {
                Position adjacentPosition = new Position(start.getRow(), end.getColumn());
                Piece adjacentPiece = board.getPiece(adjacentPosition.getRow(), adjacentPosition.getColumn());
                if (adjacentPiece instanceof Pawn) {
                    Pawn adjacentPawn = (Pawn) adjacentPiece;
                    return adjacentPawn.getColor() != movingPiece.getColor() &&
                           adjacentPawn.hasJustMovedTwoSquares();
                }
            }
        }
        return false;
    }

    public boolean makeMove(Position start, Position end) {
        Piece movingPiece = board.getPiece(start.getRow(), start.getColumn());
        if (movingPiece == null || movingPiece.getColor() != (whiteTurn ? PieceColor.WHITE : PieceColor.BLACK)) {
            return false;
        }

        boolean isEnPassantMove = isEnPassantMove(start, end, movingPiece);

        if (movingPiece.isValidMove(end, board.getBoard()) || isEnPassantMove) {
            if (isEnPassantMove) {
                executeEnPassant(start, end);
            } else {
                board.movePiece(start, end, false);
            }

            resetJustMovedTwoSquaresForPawns(movingPiece);

            String moveNotation = generateMoveNotation(start, end);
            moveHistory.add(moveNotation);
            whiteTurn = !whiteTurn;

            if (stockfishOutput != null) {
                stockfishOutput.println("position startpos moves " + String.join(" ", moveHistory));
            }
            return true;
        }
        return false;
    }

    private void resetJustMovedTwoSquaresForPawns(Piece movingPiece) {
        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece instanceof Pawn && piece != movingPiece) {
                    ((Pawn) piece).setJustMovedTwoSquares(false);
                }
            }
        }
    }

    private void executeEnPassant(Position start, Position end) {
        board.movePiece(start, end, true);
        int capturedPawnRow = start.getRow();
        int capturedPawnCol = end.getColumn();
        board.setPiece(capturedPawnRow, capturedPawnCol, null);
    }

    public String getLastMove() {
        if (!moveHistory.isEmpty()) {
            return moveHistory.get(moveHistory.size() - 1);
        }
        return null;
    }

    private String generateMoveNotation(Position start, Position end) {
        char startFile = (char) ('a' + start.getColumn());
        int startRank = 8 - start.getRow();
        char endFile = (char) ('a' + end.getColumn());
        int endRank = 8 - end.getRow();
        return "" + startFile + startRank + endFile + endRank;
    }

    public String getStockfishMove() {
        if (!isStockfishInitialized) {
            System.err.println("Stockfish is not initialized. Cannot get Stockfish move.");
            return null;
        }

        try {
            stockfishOutput.println("go movetime 1000");
            String line;
            while ((line = stockfishInput.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    return line.split(" ")[1];
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Stockfish move: " + e.getMessage());
        }
        return null;
    }

    public void playStockfishMove() {
        if (!isStockfishInitialized) {
            System.err.println("Stockfish is not initialized. Cannot play Stockfish move.");
            return;
        }

        String stockfishMove = getStockfishMove();
        if (stockfishMove != null && stockfishMove.length() == 4) {
            Position start = new Position(8 - (stockfishMove.charAt(1) - '0'), stockfishMove.charAt(0) - 'a');
            Position end = new Position(8 - (stockfishMove.charAt(3) - '0'), stockfishMove.charAt(2) - 'a');
            makeMove(start, end);
        }
    }

    public boolean isInCheck(PieceColor kingColor) {
        Position kingPosition = findKingPosition(kingColor);
        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() != kingColor) {
                    if (piece.isValidMove(kingPosition, board.getBoard())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Position findKingPosition(PieceColor color) {
        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece instanceof King && piece.getColor() == color) {
                    return new Position(row, col);
                }
            }
        }
        throw new RuntimeException("King not found, which should never happen.");
    }

    public boolean isCheckmate(PieceColor kingColor) {
        if (!isInCheck(kingColor)) {
            return false;
        }

        Position kingPosition = findKingPosition(kingColor);
        King king = (King) board.getPiece(kingPosition.getRow(), kingPosition.getColumn());

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                if (rowOffset == 0 && colOffset == 0) {
                    continue;
                }
                Position newPosition = new Position(kingPosition.getRow() + rowOffset,
                        kingPosition.getColumn() + colOffset);

                if (isPositionOnBoard(newPosition) && king.isValidMove(newPosition, board.getBoard())
                        && !wouldBeInCheckAfterMove(kingColor, kingPosition, newPosition)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPositionOnBoard(Position position) {
        return position.getRow() >= 0 && position.getRow() < board.getBoard().length &&
                position.getColumn() >= 0 && position.getColumn() < board.getBoard()[0].length;
    }

    private boolean wouldBeInCheckAfterMove(PieceColor kingColor, Position from, Position to) {
        Piece temp = board.getPiece(to.getRow(), to.getColumn());
        board.setPiece(to.getRow(), to.getColumn(), board.getPiece(from.getRow(), from.getColumn()));
        board.setPiece(from.getRow(), from.getColumn(), null);

        boolean inCheck = isInCheck(kingColor);

        board.setPiece(from.getRow(), from.getColumn(), board.getPiece(to.getRow(), to.getColumn()));
        board.setPiece(to.getRow(), to.getColumn(), temp);

        return inCheck;
    }

    public List<Position> getLegalMovesForPieceAt(Position position) {
        Piece selectedPiece = board.getPiece(position.getRow(), position.getColumn());
        if (selectedPiece == null) {
            return new ArrayList<>();
        }

        List<Position> legalMoves = new ArrayList<>();

        if (selectedPiece instanceof Pawn) {
            addPawnMoves(position, selectedPiece.getColor(), legalMoves);
        } else {
            for (int row = 0; row < board.getBoard().length; row++) {
                for (int col = 0; col < board.getBoard()[row].length; col++) {
                    Position newPos = new Position(row, col);
                    if (selectedPiece.isValidMove(newPos, board.getBoard())) {
                        legalMoves.add(newPos);
                    }
                }
            }
        }
        return legalMoves;
    }

    private void addPawnMoves(Position position, PieceColor color, List<Position> legalMoves) {
        int direction = color == PieceColor.WHITE ? -1 : 1;
        Position newPos = new Position(position.getRow() + direction, position.getColumn());
        if (isPositionOnBoard(newPos) && board.getPiece(newPos.getRow(), newPos.getColumn()) == null) {
            legalMoves.add(newPos);
        }

        if ((color == PieceColor.WHITE && position.getRow() == 6) ||
                (color == PieceColor.BLACK && position.getRow() == 1)) {
            newPos = new Position(position.getRow() + 2 * direction, position.getColumn());
            Position intermediatePos = new Position(position.getRow() + direction, position.getColumn());
            if (isPositionOnBoard(newPos) && board.getPiece(newPos.getRow(), newPos.getColumn()) == null
                    && board.getPiece(intermediatePos.getRow(), intermediatePos.getColumn()) == null) {
                legalMoves.add(newPos);
            }
        }

        for (int colOffset : new int[]{-1, 1}) {
            Position capturePos = new Position(position.getRow() + direction, position.getColumn() + colOffset);
            if (isPositionOnBoard(capturePos)) {
                Piece capturePiece = board.getPiece(capturePos.getRow(), capturePos.getColumn());
                if (capturePiece != null && capturePiece.getColor() != color) {
                    legalMoves.add(capturePos);
                } else if (isEnPassantMove(position, capturePos, board.getPiece(position.getRow(), position.getColumn()))) {
                    legalMoves.add(capturePos);
                }
            }
        }
    }

    public boolean isCastlingMove(Position start, Position end) {
        Piece movingPiece = board.getPiece(start.getRow(), start.getColumn());

        if (!(movingPiece instanceof King)) {
            return false;
        }

        King king = (King) movingPiece;
        if (king.hasMoved()) {
            return false;
        }

        int row = start.getRow();
        int colDiff = end.getColumn() - start.getColumn();

        if (Math.abs(colDiff) != 2) {
            return false;
        }

        int rookCol = (colDiff > 0) ? 7 : 0;
        Piece rook = board.getPiece(row, rookCol);

        if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) {
            return false;
        }

        int step = (colDiff > 0) ? 1 : -1;
        for (int col = start.getColumn() + step; col != rookCol; col += step) {
            if (board.getPiece(row, col) != null) {
                return false;
            }
        }

        Position middlePosition = new Position(row, start.getColumn() + step);
        if (isInCheck(king.getColor()) || wouldBeInCheckAfterMove(king.getColor(), start, middlePosition) ||
            wouldBeInCheckAfterMove(king.getColor(), start, end)) {
            return false;
        }

        return true;
    }

    public boolean isStalemate(PieceColor kingColor) {
        if (isInCheck(kingColor)) {
            return false;
        }

        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == kingColor) {
                    Position position = new Position(row, col);
                    List<Position> legalMoves = getLegalMovesForPieceAt(position);
                    if (!legalMoves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}