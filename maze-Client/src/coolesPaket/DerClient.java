
package coolesPaket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;

import javax.xml.bind.UnmarshalException;
import javax.xml.stream.XMLOutputFactory;

import de.fhac.mazenet.server.Board;
import de.fhac.mazenet.server.Card;
import de.fhac.mazenet.server.Game;
import de.fhac.mazenet.server.Player;
import de.fhac.mazenet.server.Position;
import de.fhac.mazenet.server.networking.Connection;
import de.fhac.mazenet.server.networking.MazeComMessageFactory;
import de.fhac.mazenet.server.networking.XmlInStream;
import de.fhac.mazenet.server.networking.XmlOutStream;
import de.fhac.mazenet.server.userinterface.mazeFX.objects.TreasureFX;
import de.fhac.mazenet.server.generated.AwaitMoveMessageType;
import de.fhac.mazenet.server.generated.BoardType;
import de.fhac.mazenet.server.generated.CardType;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComType;
import de.fhac.mazenet.server.generated.MoveMessageType;
import de.fhac.mazenet.server.generated.ObjectFactory;
import de.fhac.mazenet.server.generated.PositionType;
import de.fhac.mazenet.server.generated.TreasureType;

public class DerClient {

	private static int PORT = 5123;
	private static String ip;// = "localhost";
	private static XmlOutStream xos;
	private static XmlInStream xin;
	private static boolean eingeloggt = true;
	private static ObjectFactory of = new ObjectFactory();
	private static int ourID;

	private static MazeCom createMoveMessage(MoveMessageType mmt) {
		MazeCom mc = of.createMazeCom();
		mc.setId(ourID);
		mc.setMcType(MazeComType.MOVE);
		// MoveMessageType mmt = of.createMoveMessageType();

		mc.setMoveMessage(mmt);
		// mc.getMoveMessage().setShiftCard();

		return mc;
	}

	public static void main(String argv[]) throws UnknownHostException, IOException, UnmarshalException {
		if (argv.length > 1) {
			ip = argv[1];
		}
		// String path = "/home/rn084191/Rechnernetze/truststore.jks";
		// String prop = "javax.net.ssl.trustStore";
		// System.setProperty(propd, path);
		// System.setProperty("javax.net.ssl.trustStorePassword", "geheim");
		System.out.println("client starts");
		Socket s = new Socket(ip, PORT);
		xos = new XmlOutStream(s.getOutputStream());
		xin = new XmlInStream(s.getInputStream());

		MazeCom mc = MazeComMessageFactory.createLoginMessage("TeamGut");
		xos.write(mc);

		while (eingeloggt) {
			mc = xin.readMazeCom();
			switch (mc.getMcType()) {
			case ACCEPT:

				System.out.println("Ich bin in ACCEPT");
				break;
			case AWAITMOVE:
				System.out.println("Ich bin in AWAITMOVE");
				AwaitMoveMessageType awaitMoveMessage = mc.getAwaitMoveMessage();
				CardType shiftCard = awaitMoveMessage.getBoard().getShiftCard();
				BoardType board = awaitMoveMessage.getBoard();
				TreasureType treasure = awaitMoveMessage.getTreasure();

				// MoveMessageType mmt = of.createMoveMessageType();
				// board.getForbidden()
				Board b = new Board(board);
				Card c = new Card(shiftCard);
				MoveMessageType mmt = getBestMove(ourID, b, treasure, c);

				xos.write(createMoveMessage(mmt));

				// sendva
				break;
			case DISCONNECT:
				System.out.println("Ich bin in DISCONNECT");
				break;
			case LOGIN:
				System.err.println("Fehler: LOGIN");
				break;
			case LOGINREPLY:
				ourID = mc.getLoginReplyMessage().getNewID();
				System.out.println(ourID);
				System.out.println("Ich bin in LOGINREPLY");
				break;
			case MOVE:
				System.err.println("Fehler: Move");
				break;
			case WIN:
				System.out.println("Ich bin in WIN");
				String winner = mc.getWinMessage().getWinner().toString();
				System.out.println(winner + " hat gewonnen.");
				System.exit(0);

				break;
			default:
				break;
			}

		}
	}

	private static int[] getGoal(Board b, TreasureType treasure) { /*if(puehdragoran(pt, goal)!=0){
		 if(pt.getCol()%6==0 || pt.getRow()%6==0){
		 break;
	 }
}*/

		int[] goal = new int[2];
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 7; j++) {
				if (b.getCard(i, j).getTreasure() != null) {
					if (b.getCard(i, j).getTreasure().compareTo(treasure) == 0) {
						goal[0] = i;
						goal[1] = j;
					}
				}
			}
		}
		return goal;
	}

	private static GoVals go(int id, Board b, int[] goal, Card c) {
		double best=Double.MAX_VALUE;
		PositionType pt_best=new PositionType();
		PositionType cardpos_best=new PositionType();
		Card card_best=new Card(c);
		int[] shift_best=new int[2];
		
		ArrayList<int[]> shiftpos = new ArrayList<>();
		for (int i = 1; i < 7; i = i + 2) {
			shiftpos.add(new int[] { i, 0 });
			shiftpos.add(new int[] { 0, i });
			shiftpos.add(new int[] { 6, i });
			shiftpos.add(new int[] { i, 6 });

		}
		if (b.getForbidden() != null) {
			for (int[] i : shiftpos) {
				if (b.getForbidden().getRow() == i[0] && b.getForbidden().getCol() == i[1]) {
					shiftpos.remove(i);
					break;
				}
			}
		}
	
		for (Card karte : c.getPossibleRotations()) {

			for (int[] pos : shiftpos) {
				
				MoveMessageType mmt = new MoveMessageType();
				mmt.setShiftCard(karte);
				mmt.setShiftPosition(new Position(pos[0], pos[1]));							
				Board boardAfterMove = b.fakeShift(mmt);
				
				for (PositionType pt : boardAfterMove.getAllReachablePositions(boardAfterMove.findPlayer(id))) {
					 if (puehdragoran(pt, goal)<=best){
						
						 best=puehdragoran(pt, goal);
						 pt_best=pt;
						 card_best=karte;
						 shift_best=pos;
						 cardpos_best.setRow(pos[0]);
						 cardpos_best.setCol(pos[1]);
					 }
				}
			}

		}
		GoVals gv =new DerClient().new GoVals(card_best, cardpos_best,pt_best);
		
		return gv;
	}
	
	private static double puehdragoran(PositionType pt, int[] goal){
		return Math.pow(pt.getRow()-goal[0],2)+Math.pow(pt.getCol()-goal[1], 2);
	}
	
	private static MoveMessageType getBestMove(int id, Board b, TreasureType treasure, Card c) {
		MoveMessageType mmt = of.createMoveMessageType();
		PositionType pinpos = new PositionType();
		PositionType shiftpos = new PositionType();

		int[] goal = getGoal(b, treasure);
		GoVals gov=go(id, b, goal, c);

		pinpos=gov.getPinPos();
		shiftpos=gov.getShiftpos();
		mmt.setNewPinPos(pinpos);
		mmt.setShiftCard(gov.getCard());
		mmt.setShiftPosition(shiftpos);
		return mmt;

	}

	public class GoVals {
		private Card card;
		private PositionType shiftpos;
		private PositionType pinpos;
			
		public GoVals(Card c, PositionType shiftpos, PositionType pinpos){
			this.card = c;
			this.shiftpos = shiftpos;
			this.pinpos = pinpos;
			System.out.println("row="+shiftpos.getRow());
			System.out.println("col="+shiftpos.getCol());
			System.out.println(c.toString());
			
		}
		
		public Card getCard(){
			return this.card;
		}
		
		public PositionType getShiftpos(){
			return this.shiftpos;
		} 
		
		public PositionType getPinPos(){
			return this.pinpos;
		} 
	}

}
