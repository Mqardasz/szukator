package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainFrame {
    private JFrame frame;
    private static final String DIR_PATH = "C:\\Users\\micha\\Desktop\\studia\\Semestr_III\\Programowanie_obiektowe\\synchronizator\\szukator\\slowa";
    private final int liczbaWyrazowStatystyki = 4;
    private final AtomicBoolean fajrant = new AtomicBoolean(false);
    private final int liczbaProducentow = 1;
    private final int liczbaKonsumentow = 2;
    private final int przerwa = 10;

    private ExecutorService executor = Executors.newFixedThreadPool(liczbaProducentow + liczbaKonsumentow);
    private List<Future<?>> producentFuture = new ArrayList<>();
    private List<Future<?>> konsumentFuture = new ArrayList<>();

    private BlockingQueue<Optional<Path>> kolejka; // Accessible for poison pills

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                MainFrame window = new MainFrame();
                window.frame.pack();
                window.frame.setAlwaysOnTop(true);
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MainFrame() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Multi-threaded Word Statistics");
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });

        JPanel panel = new JPanel();
        frame.add(panel, BorderLayout.NORTH);

        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(e -> getMultiThreadedStatistics());
        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(e -> stopAllTasks());
        JButton btnZamknij = new JButton("Zamknij");
        btnZamknij.addActionListener(e -> {
            executor.shutdownNow();
            frame.dispose();
        });

        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnZamknij);
    }

    private void stopAllTasks() {
        fajrant.set(true);

        // Cancel producer tasks
        for (Future<?> f : producentFuture) {
            f.cancel(true);
        }

        // Send poison pills to consumers
        if (kolejka != null) {
            for (int i = 0; i < liczbaKonsumentow; i++) {
                try {
                    kolejka.put(Optional.empty()); // Poison pill
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // No need to cancel consumer threads explicitly — they will stop gracefully
    }

    private void getMultiThreadedStatistics() {
        if (tasksRunning(producentFuture, "producent") || tasksRunning(konsumentFuture, "konsument")) {
            return;
        }

        fajrant.set(false);
        producentFuture.clear();
        konsumentFuture.clear();

        kolejka = new LinkedBlockingQueue<>(liczbaKonsumentow);

        for (int i = 0; i < liczbaProducentow; i++) {
            Runnable producer = new Producer(fajrant, kolejka, DIR_PATH, przerwa);
            producentFuture.add(executor.submit(producer));
        }

        for (int i = 0; i < liczbaKonsumentow; i++) {
            Runnable consumer = new Consumer(fajrant, kolejka, liczbaWyrazowStatystyki);
            konsumentFuture.add(executor.submit(consumer));
        }
    }

    private boolean tasksRunning(List<Future<?>> futures, String role) {
        for (Future<?> f : futures) {
            if (!f.isDone()) {
                JOptionPane.showMessageDialog(frame,
                        "Nie można uruchomić nowego zadania!\nPrzynajmniej jeden " + role + " nadal działa!",
                        "OSTRZEŻENIE", JOptionPane.WARNING_MESSAGE);
                return true;
            }
        }
        return false;
    }
}