package de.sub.goobi.Forms;

import java.util.LinkedList;

import de.sub.goobi.Beans.Prozess;
//import de.sub.goobi.Persistence.ProzessDAO;
//import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.tasks.LongRunningTask;
import de.sub.goobi.helper.tasks.LongRunningTaskManager;
//import de.sub.goobi.helper.tasks.ProcessSwapInTask;
//import de.sub.goobi.helper.tasks.ProcessSwapOutTask;

public class LongRunningTasksForm {
   private Prozess prozess;
   private LongRunningTask task;

   public LinkedList<LongRunningTask> getTasks() {
      return LongRunningTaskManager.getInstance().getTasks();
   }
   /*
   public void addNewSwapOutTask() throws DAOException {
      Prozess p = new ProzessDAO().get(118);
      task = new ProcessSwapOutTask();
      task.initialize(p);
      LongRunningTaskManager.getInstance().addTask(task);
   }
   
   public void addNewSwapInTask() throws DAOException {
      Prozess p = new ProzessDAO().get(118);
      task = new ProcessSwapInTask();
      task.initialize(p);
      LongRunningTaskManager.getInstance().addTask(task);
   }
    */

   public void addNewMasterTask() {
      Prozess p = new Prozess();
      p.setTitel("hallo Titel " + System.currentTimeMillis());
      task = new LongRunningTask();
      task.initialize(p);
      LongRunningTaskManager.getInstance().addTask(task);
   }

   /**
    * Thread entweder starten oder restarten
   * ================================================================*/
   public void executeTask() {
      if (task.getStatusProgress() == 0)
         LongRunningTaskManager.getInstance().executeTask(task);
      else {
         /* Thread lief schon und wurde abgebrochen */
         try {
            LongRunningTask lrt = task.getClass().newInstance();
            lrt.initialize(task.getProzess());
            LongRunningTaskManager.getInstance().replaceTask(task, lrt);
            LongRunningTaskManager.getInstance().executeTask(lrt);
         } catch (InstantiationException e) {
            e.printStackTrace();
         } catch (IllegalAccessException e) {
            e.printStackTrace();
         }
      }
   }

   
   public void clearFinishedTasks() {
      LongRunningTaskManager.getInstance().clearFinishedTasks();
   }
   
   public void clearAllTasks() {
      LongRunningTaskManager.getInstance().clearAllTasks();
   }

   public void moveTaskUp() {
      LongRunningTaskManager.getInstance().moveTaskUp(task);
   }

   public void moveTaskDown() {
      LongRunningTaskManager.getInstance().moveTaskDown(task);
   }
   
   public void cancelTask() {
      LongRunningTaskManager.getInstance().cancelTask(task);
   }

   public void removeTask() {
      LongRunningTaskManager.getInstance().removeTask(task);
   }

   public Prozess getProzess() {
      return prozess;
   }

   public void setProzess(Prozess prozess) {
      this.prozess = prozess;
   }

   public LongRunningTask getTask() {
      return task;
   }

   public void setTask(LongRunningTask task) {
      this.task = task;
   }

   public boolean isRunning() {
      return LongRunningTaskManager.getInstance().isRunning();
   }

   public void toggleRunning() {
      LongRunningTaskManager.getInstance().setRunning(!LongRunningTaskManager.getInstance().isRunning());
   }
}