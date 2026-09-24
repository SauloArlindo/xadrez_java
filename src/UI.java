import chess.ChessPiece;

public class UI {

    pubic static void printBoard(ChessPiece[][] pieces){
        for(int i = 0 ; i < pieces.length; i++){
            System.out.print((8 - 1) + " ");
            for(int j = 0 ; j < pieces.length; j++){
                System.out.print(pieces[i][j]);
            }
            System.out.println();
        }
        System.out.println("a b c d e f g h");
        
    }

    private static void printPiece(ChessPiece piece){
        if(piece == null){
            System.out.print("-");
        }else{
            System.out.print(piece);
        }
        System.err.print(" ");
    }
}
