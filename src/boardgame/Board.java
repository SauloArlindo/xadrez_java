package boardgame;

public class Board {
    private int rows;
    private int columns;
    private Piece[][] pieces;

    public Board(int columns, int rows) {
        if (rows < 1 || columns < 1){
            throw new BoardException("Deve ter pelo menos 1 coluna e 1 linha");
        }
        this.columns = columns;
        this.rows = rows;
        pieces = new Piece[rows][rows];
    }

    public int getRows() {
        return rows;
    }
    public int getColumns() {
        return columns;
    }

    public Piece piece(int row,int column){
        if(!positionExists(row,column)){
            throw new BoardException("Posição fora do tabuleiro");
        }
        return pieces[row][column];
    }

    public Piece piece(Position position){
        if(!positionExists(position)){
            throw new BoardException("Posição fora do tabuleiro");
        }
        return pieces[position.getRow()][position.getColumn()];
    }

    public void placePiece(Piece piece , Position position){
        if (thereIsAPiece(position)){
            throw new BoardException("Ja tem uma peça neste local");
        }
        pieces[position.getRow()][position.getColumn()] = piece;
        piece.position = position;
    }
    private boolean positionExists(int row , int column){
        return row >= 0 && row < rows && column >= 0  && column < columns;
    }

    public boolean positionExists(Position position){
        return positionExists(position.getRow() , position.getColumn());
    }
    public boolean thereIsAPiece(Position position){
        if(!positionExists(position)){
            throw new BoardException("Posição fora do tabuleiro");
        }
        return piece(position) != null;
    }

}
