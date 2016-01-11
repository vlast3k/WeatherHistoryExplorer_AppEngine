package com.vladi.gae1;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class FeedbackServlet extends HttpServlet {
	public FeedbackServlet() {
		log.setLevel(Level.INFO);
	}
	private static Logger log = Logger.getLogger(FeedbackServlet.class.getName());
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		resp.getWriter().print("Feedback Servlet");
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {
		try {
			log.info(Utils.readTextInputStream(req.getInputStream()));
		} catch (Exception e) {
			e.printStackTrace(resp.getWriter());
			log.severe(e.toString());
			throw new ServletException(e);
		}
	}
}
