import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.Color;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Interface gráfica (Swing) para o jogo de xadrez.
 * Reaproveita 100% da lógica de ChessMatch; só troca Scanner/System.out por mouse/desenho.
 */
public class ChessGUI extends JFrame {

    private final ChessMatch match = new ChessMatch();
    private final List<ChessPiece> captured = new ArrayList<>();

    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel capturedWhite = new JLabel(" ");
    private final JLabel capturedBlack = new JLabel(" ");

    // estado da seleção (índices de matriz: linha 0 = rank 8)
    private int selRow = -1, selCol = -1;
    private boolean[][] possibleMoves = null;

    public ChessGUI() {
        super("Xadrez");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel south = new JPanel(new GridLayout(2, 1));
        south.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        south.add(capturedWhite);
        south.add(capturedBlack);

        add(statusLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        updateStatus(null);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---------------------------------------------------------------- lógica de clique

    private void handleClick(int row, int col) {
        if (match.getCheckMate()) return;

        // 2º clique: tenta mover se a casa clicada é um destino válido
        if (selRow >= 0 && possibleMoves != null && possibleMoves[row][col]) {
            tryMove(row, col);
            return;
        }
        // 1º clique (ou troca de peça): seleciona
        select(row, col);
    }

    private void select(int row, int col) {
        try {
            possibleMoves = match.possibleMoves(toChessPosition(row, col));
            selRow = row;
            selCol = col;
            updateStatus(null);
        } catch (ChessException ex) {
            clearSelection();
            updateStatus(ex.getMessage());
        }
        boardPanel.repaint();
    }

    private void tryMove(int row, int col) {
        try {
            ChessPiece cap = match.performChessMove(
                    toChessPosition(selRow, selCol), toChessPosition(row, col));
            if (cap != null) captured.add(cap);

            if (match.getPromoted() != null) {
                String type = askPromotion();
                match.replacePromotedPiece(type);
            }
            clearSelection();
            updateStatus(null);
            updateCaptured();
            boardPanel.repaint();

            if (match.getCheckMate()) {
                JOptionPane.showMessageDialog(this,
                        "XEQUE-MATE! Vencedor: " + nomeCor(match.getCurrentPlayer()),
                        "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (ChessException ex) {
            clearSelection();
            updateStatus(ex.getMessage());
            boardPanel.repaint();
        }
    }

    private String askPromotion() {
        String[] nomes = {"Rainha", "Torre", "Bispo", "Cavalo"};
        String[] codigos = {"Q", "R", "B", "N"};
        int r = JOptionPane.showOptionDialog(this, "Promover peão para:", "Promoção",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, nomes, nomes[0]);
        return (r >= 0) ? codigos[r] : "Q";
    }

    private void clearSelection() {
        selRow = selCol = -1;
        possibleMoves = null;
    }

    // ---------------------------------------------------------------- textos

    private void updateStatus(String erro) {
        if (erro != null) {
            statusLabel.setText(erro);
            statusLabel.setForeground(new java.awt.Color(180, 30, 30));
            return;
        }
        statusLabel.setForeground(java.awt.Color.DARK_GRAY);
        if (match.getCheckMate()) {
            statusLabel.setText("XEQUE-MATE! Vencedor: " + nomeCor(match.getCurrentPlayer()));
        } else {
            String s = "Turno " + match.getTurn() + " — vez das " + nomeCor(match.getCurrentPlayer());
            if (match.getCheck()) s += "  (XEQUE!)";
            statusLabel.setText(s);
        }
    }

    private void updateCaptured() {
        StringBuilder w = new StringBuilder("Brancas capturadas: ");
        StringBuilder b = new StringBuilder("Pretas capturadas: ");
        for (ChessPiece p : captured) {
            (p.getColor() == Color.WHITE ? w : b).append(glyph(p)).append(' ');
        }
        capturedWhite.setText(w.toString());
        capturedBlack.setText(b.toString());
    }

    private static String nomeCor(Color c) {
        return c == Color.WHITE ? "BRANCAS" : "PRETAS";
    }

    private static ChessPosition toChessPosition(int row, int col) {
        return new ChessPosition((char) ('a' + col), 8 - row);
    }

    private static String glyph(ChessPiece p) {
        switch (p.toString()) {
            case "K": return "\u265A";
            case "Q": return "\u265B";
            case "R": return "\u265C";
            case "B": return "\u265D";
            case "N": return "\u265E";
            default:  return "\u265F"; // peão
        }
    }

    // ---------------------------------------------------------------- tabuleiro

    private class BoardPanel extends JPanel {
        private static final int MARGIN = 24;           // espaço p/ coordenadas
        private final java.awt.Color LIGHT = new java.awt.Color(240, 217, 181);
        private final java.awt.Color DARK = new java.awt.Color(181, 136, 99);
        private final java.awt.Color SELECTED = new java.awt.Color(246, 246, 105);
        private final java.awt.Color CHECK = new java.awt.Color(230, 80, 80);

        BoardPanel() {
            setPreferredSize(new Dimension(640, 640));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int size = squareSize();
                    int col = (e.getX() - MARGIN) / size;
                    int row = (e.getY() - MARGIN) / size;
                    if (e.getX() >= MARGIN && e.getY() >= MARGIN
                            && row >= 0 && row < 8 && col >= 0 && col < 8) {
                        handleClick(row, col);
                    }
                }
            });
        }

        private int squareSize() {
            return (Math.min(getWidth(), getHeight()) - 2 * MARGIN) / 8;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = squareSize();
            ChessPiece[][] pieces = match.getPieces();

            // coordenadas
            g.setColor(java.awt.Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < 8; i++) {
                String letra = String.valueOf((char) ('a' + i));
                g.drawString(letra, MARGIN + i * size + (size - fm.stringWidth(letra)) / 2, MARGIN - 8);
                String num = String.valueOf(8 - i);
                g.drawString(num, 8, MARGIN + i * size + (size + fm.getAscent()) / 2 - 2);
            }

            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int x = MARGIN + j * size;
                    int y = MARGIN + i * size;

                    g.setColor((i + j) % 2 == 0 ? LIGHT : DARK);
                    g.fillRect(x, y, size, size);

                    if (i == selRow && j == selCol) {
                        g.setColor(SELECTED);
                        g.fillRect(x, y, size, size);
                    }

                    // rei em xeque
                    ChessPiece p = pieces[i][j];
                    if (p != null && match.getCheck() && p.toString().equals("K")
                            && p.getColor() == match.getCurrentPlayer()) {
                        g.setColor(CHECK);
                        g.fillRect(x, y, size, size);
                    }

                    if (p != null) drawPiece(g, p, x, y, size);

                    // destinos possíveis
                    if (possibleMoves != null && possibleMoves[i][j]) {
                        g.setColor(new java.awt.Color(40, 120, 220, 150));
                        if (p == null) {
                            int d = size / 3;
                            g.fillOval(x + (size - d) / 2, y + (size - d) / 2, d, d);
                        } else {
                            g.setStroke(new BasicStroke(5f));
                            g.drawOval(x + 4, y + 4, size - 8, size - 8);
                        }
                    }
                }
            }
            g.dispose();
        }

        private void drawPiece(Graphics2D g, ChessPiece p, int x, int y, int size) {
            Font font = new Font("Segoe UI Symbol", Font.PLAIN, (int) (size * 0.8));
            if (!font.canDisplay('\u265A')) {
                font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) (size * 0.8));
            }
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout tl = new TextLayout(glyph(p), font, frc);
            Rectangle2D b = tl.getBounds();
            float tx = (float) (x + (size - b.getWidth()) / 2 - b.getX());
            float ty = (float) (y + (size - b.getHeight()) / 2 - b.getY());
            Shape outline = tl.getOutline(AffineTransform.getTranslateInstance(tx, ty));

            // glifo cheio nas duas cores; cor + contorno diferenciam os lados
            g.setColor(p.getColor() == Color.WHITE ? java.awt.Color.WHITE : java.awt.Color.BLACK);
            g.fill(outline);
            g.setColor(p.getColor() == Color.WHITE ? java.awt.Color.BLACK : new java.awt.Color(230, 230, 230));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(outline);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}
