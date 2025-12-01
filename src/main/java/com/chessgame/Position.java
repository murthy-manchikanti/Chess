package com.chessgame;

public class Position {
    private int row;
    private int column;
  
    public Position(int row, int column) {
        this.row = row;
        this.column = column;
    }
  
    public int getRow() {
        return row;
    }
  
    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
    return toChessNotation();
    }

    public String toChessNotation() {
        char file = (char) ('a' + column);  // Convert column index to letter (a-h)
        int rank = 8 - row;  // Convert row index to chess rank (1-8)
        return "" + file + rank;
    }

}
