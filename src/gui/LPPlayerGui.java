package gui;

import com.jtattoo.plaf.bernstein.BernsteinLookAndFeel;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import objects.MP3Player;
import objects.MP3;
import utils.FileUtils;
import utils.MP3PlayerFileFilter;
import utils.SkinUtils;

/**
 *
 * @author Musichen
 */
public class LPPlayerGui extends javax.swing.JFrame implements BasicPlayerListener {

    //<editor-fold defaultstate="collapsed" desc="константы">
    private static final String MP3_FILE_EXTENSION = "mp3";
    private static final String MP3_FILE_DESCRIPTION = "files mp3";
    private static final String PLAYLIST_FILE_EXTENSION = "pls";
    private static final String PLAYLIST_FILE_DESCRIPTION = "Files of playlist";
    private static final String EMPTY_STRING = "";
    private static final String INPUT_SONG_NAME = "eneter a song name";
    //</editor-fold>
    
    private DefaultListModel mp3ListModel = new DefaultListModel();
    private FileFilter mp3FileFilter = new MP3PlayerFileFilter(MP3_FILE_EXTENSION, MP3_FILE_DESCRIPTION);
    private FileFilter playlistFileFilter = new MP3PlayerFileFilter(PLAYLIST_FILE_EXTENSION, PLAYLIST_FILE_DESCRIPTION);
    private MP3Player player = new MP3Player(this);
    
    //<editor-fold defaultstate="collapsed" desc="переменные для прокрутки песни">
    private long secondsAmount; // сколько секунд прошло с начала проигрывания
    private long duration; // длительность песни в секундах
    private int bytesLen; // размер песни в байтах
    private double posValue = 0.0; // позиция для прокрутки
    // передвигается ли ползунок песни от перетаскивания (или от проигрывания) - используется во время перемотки
    private boolean movingFromJump = false;
    private boolean moveAutomatic = false;// во время проигрывании песни ползунок передвигается, moveAutomatic = true
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="слушатель событий плеера BasicPlayerListener">
    @Override
    public void opened(Object o, Map map) {

        //        еще один вариант определения mp3 тегов
//        AudioFileFormat aff = null;
//        try {
//            aff = AudioSystem.getAudioFileFormat(new File(o.toString()));
//        } catch (UnsupportedAudioFileException ex) {
//            Logger.getLogger(LPPlayerGui.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(LPPlayerGui.class.getName()).log(Level.SEVERE, null, ex);
//        }

        
        // определить длину песни и размер файла
        duration = (long) Math.round((((Long) map.get("duration")).longValue()) / 1000000);
        bytesLen = (int) Math.round(((Integer) map.get("mp3.length.bytes")).intValue());

        // если есть mp3 тег для имени - берем его, если нет - вытаскиваем название из имени файла
        String songName = map.get("title") != null ? map.get("title").toString() : FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());

        // если длинное название - укоротить его
        if (songName.length() > 30) {
            songName = songName.substring(0, 30) + "...";
        }

        labelSongName.setText(songName);

    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {

        float progress = -1.0f;

        if ((bytesread > 0) && ((duration > 0))) {
            progress = bytesread * 1.0f / bytesLen * 1.0f;
        }


        // сколько секунд прошло
        secondsAmount = (long) (duration * progress);

        if (duration != 0) {
            if (movingFromJump == false) {
                slideProgress.setValue(((int) Math.round(secondsAmount * 1000 / duration)));

            }
        }
    }

    @Override
    public void stateUpdated(BasicPlayerEvent bpe) {
        int state = bpe.getCode();

        if (state == BasicPlayerEvent.PLAYING) {
            movingFromJump = false;
        } else if (state == BasicPlayerEvent.SEEKING) {
            movingFromJump = true;
        } else if (state == BasicPlayerEvent.EOM) {
            if (selectNextSong()) {
                playFile();
            }
        }


    }

    @Override
    public void setController(BasicController bc) {
    }
    //</editor-fold>

    /**
     * Creates new form LPPlayerGui
     */
    public LPPlayerGui() {
        initComponents();

    }

    private void playFile() {
        int[] indexPlayList = lstPlayList.getSelectedIndices();// получаем выбранные индексы(порядковый номер) песен
        if (indexPlayList.length > 0) {// если выбрали хотя бы одну песню
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[0]);// находим первую выбранную песню (т.к. несколько песен нельзя проиграть одновременно
            player.play(mp3.getPath());
            player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());
        }

    }

    private boolean selectPrevSong() {
        int nextIndex = lstPlayList.getSelectedIndex() - 1;
        if (nextIndex >= 0) {// если не вышли за пределы плейлиста
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }

        return false;
    }

    private boolean selectNextSong() {
        int nextIndex = lstPlayList.getSelectedIndex() + 1;
        if (nextIndex <= lstPlayList.getModel().getSize() - 1) {// если не вышли за пределы плейлиста
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }
        return false;
    }

    private void searchSong() {
        String searchStr = txtSearch.getText();

        // если в поиске ничего не ввели - выйти из метода и не производить поиск
        if (searchStr == null || searchStr.trim().equals(EMPTY_STRING)) {
            return;
        }

        // все индексы объектов, найденных по поиску, будут храниться в коллекции
        ArrayList<Integer> mp3FindedIndexes = new ArrayList<Integer>();

        // проходим по коллекции и ищем соответствия имен песен со строкой поиска
        for (int i = 0; i < mp3ListModel.size(); i++) {
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(i);
            // поиск вхождения строки в название песни без учета регистра букв
            if (mp3.getName().toUpperCase().contains(searchStr.toUpperCase())) {
                mp3FindedIndexes.add(i);// найденный индексы добавляем в коллекцию
            }
        }

        // коллекцию индексов сохраняем в массив
        int[] selectIndexes = new int[mp3FindedIndexes.size()];

        if (selectIndexes.length == 0) {// если не найдено ни одной песни, удовлетворяющей условию поиска
            JOptionPane.showMessageDialog(this, "Поиск по строке \'" + searchStr + "\' не дал результатов");
            txtSearch.requestFocus();
            txtSearch.selectAll();
            return;
        }

        // преобразовать коллекцию в массив, т.к. метод для выделения строк в JList работает только с массивом
        for (int i = 0; i < selectIndexes.length; i++) {
            selectIndexes[i] = mp3FindedIndexes.get(i).intValue();
        }

        // выделить в плелисте найдные песни по массиву индексов, найденных ранее
        lstPlayList.setSelectedIndices(selectIndexes);
    }

    private void processSeek(double bytes) {
        try {
            long skipBytes = (long) Math.round(((Integer) bytesLen).intValue() * bytes);
            player.jump(skipBytes);
        } catch (Exception e) {
            e.printStackTrace();
            movingFromJump = false;
        }

    }
    
    private void setIconImage(ImageIcon icon) {
    icon = new javax.swing.ImageIcon(getClass().getResource("/images/icon.png"));
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        popupPlaylist = new javax.swing.JPopupMenu();
        popmenuAddSong = new javax.swing.JMenuItem();
        popmenuDeleteSong = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        popmenuOpenPlaylist = new javax.swing.JMenuItem();
        popmenuClearPlaylist = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        popmenuPlay = new javax.swing.JMenuItem();
        popmenuStop = new javax.swing.JMenuItem();
        popmenuPause = new javax.swing.JMenuItem();
        panelMain = new javax.swing.JPanel();
        btnStopSong = new javax.swing.JButton();
        btnPauseSong = new javax.swing.JButton();
        btnPlaySong = new javax.swing.JButton();
        btnNextSong = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstPlayList = new javax.swing.JList();
        slideVolume = new javax.swing.JSlider();
        tglbtnVolume = new javax.swing.JToggleButton();
        btnPrevSong = new javax.swing.JButton();
        btnAddSong = new javax.swing.JButton();
        btnDeleteSong = new javax.swing.JButton();
        btnSelectNext = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        btnSelectPrev = new javax.swing.JButton();
        slideProgress = new javax.swing.JSlider();
        labelSongName = new javax.swing.JLabel();
        panelSearch = new javax.swing.JPanel();
        btnSearch = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuOpenPlaylist = new javax.swing.JMenuItem();
        menuSavePlaylist = new javax.swing.JMenuItem();
        menuSeparator = new javax.swing.JPopupMenu.Separator();
        menuExit = new javax.swing.JMenuItem();
        menuPrefs = new javax.swing.JMenu();
        menuChangeSkin = new javax.swing.JMenu();
        menuSkin1 = new javax.swing.JMenuItem();
        menuSkin2 = new javax.swing.JMenuItem();

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Выбрать файл");
        fileChooser.setMultiSelectionEnabled(true);

        popmenuAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        popmenuAddSong.setText("Добавить песню");
        popmenuAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuAddSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuAddSong);

        popmenuDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        popmenuDeleteSong.setText("Удалить песню");
        popmenuDeleteSong.setToolTipText("");
        popmenuDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuDeleteSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuDeleteSong);
        popupPlaylist.add(jSeparator1);

        popmenuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        popmenuOpenPlaylist.setText("Открыть плейлист");
        popmenuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuOpenPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuOpenPlaylist);

        popmenuClearPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/clear.png"))); // NOI18N
        popmenuClearPlaylist.setText("Очистить плейлист");
        popmenuClearPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuClearPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuClearPlaylist);
        popupPlaylist.add(jSeparator3);

        popmenuPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        popmenuPlay.setText("Play");
        popmenuPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPlayActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPlay);

        popmenuStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        popmenuStop.setText("Stop");
        popmenuStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuStopActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuStop);

        popmenuPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        popmenuPause.setText("Pause");
        popmenuPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPauseActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPause);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("LP Player                                             by musichen.co");
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/icon.png")));
        setResizable(false);

        panelMain.setBorder(new javax.swing.border.MatteBorder(null));

        btnStopSong.setForeground(new java.awt.Color(0,0,255));
        btnStopSong.setBackground(new java.awt.Color(0,0,0));
        btnStopSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        btnStopSong.setToolTipText("Stop song");
        btnStopSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopSongActionPerformed(evt);
            }
        });

        btnPauseSong.setForeground(new java.awt.Color(0,0,255));
        btnPauseSong.setBackground(new java.awt.Color(0,0,0));
        btnPauseSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        btnPauseSong.setToolTipText("Pause");
        btnPauseSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseSongActionPerformed(evt);
            }
        });

        btnPlaySong.setForeground(new java.awt.Color(0,0,255));
        btnPlaySong.setBackground(new java.awt.Color(0,0,0));
        btnPlaySong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        btnPlaySong.setToolTipText("Play song");
        btnPlaySong.setName("btnPlay"); // NOI18N
        btnPlaySong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlaySongActionPerformed(evt);
            }
        });

        btnNextSong.setForeground(new java.awt.Color(0,0,255));
        btnNextSong.setBackground(new java.awt.Color(0,0,0));
        btnNextSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/next-icon.png"))); // NOI18N
        btnNextSong.setToolTipText("Next song");
        btnNextSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextSongActionPerformed(evt);
            }
        });

        lstPlayList.setModel(mp3ListModel);
        lstPlayList.setToolTipText("Playlist");
        lstPlayList.setComponentPopupMenu(popupPlaylist);
        lstPlayList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstPlayListMouseClicked(evt);
            }
        });
        lstPlayList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                lstPlayListKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(lstPlayList);

        slideVolume.setMaximum(200);
        slideVolume.setMinorTickSpacing(5);
        slideVolume.setOrientation(javax.swing.JSlider.VERTICAL);
        slideVolume.setSnapToTicks(true);
        slideVolume.setToolTipText("Volume");
        slideVolume.setValue(150);
        slideVolume.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        slideVolume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideVolumeStateChanged(evt);
            }
        });

        tglbtnVolume.setForeground(new java.awt.Color(0,0,255));
        tglbtnVolume.setBackground(new java.awt.Color(0,0,0));
        tglbtnVolume.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/speaker.png"))); // NOI18N
        tglbtnVolume.setToolTipText("Mute sound");
        tglbtnVolume.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mute.png"))); // NOI18N
        tglbtnVolume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglbtnVolumeActionPerformed(evt);
            }
        });

        btnPrevSong.setForeground(new java.awt.Color(0,0,255));
        btnPrevSong.setBackground(new java.awt.Color(0,0,0));
        btnPrevSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/prev-icon.png"))); // NOI18N
        btnPrevSong.setToolTipText("Prev song");
        btnPrevSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrevSongActionPerformed(evt);
            }
        });

        btnAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        btnAddSong.setToolTipText("Add song");
        btnAddSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnAddSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnAddSong.setName("btnAddSong"); // NOI18N
        btnAddSong.setForeground(new java.awt.Color(0,0,255));
        btnAddSong.setBackground(new java.awt.Color(0,0,0));
        btnAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddSongActionPerformed(evt);
            }
        });

        btnDeleteSong.setForeground(new java.awt.Color(0,0,255));
        btnDeleteSong.setBackground(new java.awt.Color(0,0,0));
        btnDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        btnDeleteSong.setToolTipText("Delete song");
        btnDeleteSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setName("btnAddSong"); // NOI18N
        btnDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteSongActionPerformed(evt);
            }
        });

        btnSelectNext.setForeground(new java.awt.Color(0,0,255));
        btnSelectNext.setBackground(new java.awt.Color(0,0,0));
        btnSelectNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-down-icon.png"))); // NOI18N
        btnSelectNext.setToolTipText("Navigate Down in playlist");
        btnSelectNext.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setName("btnAddSong"); // NOI18N
        btnSelectNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectNextActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setToolTipText("Navigate Up in playlist");

        btnSelectPrev.setForeground(new java.awt.Color(0,0,255));
        btnSelectPrev.setBackground(new java.awt.Color(0,0,0));
        btnSelectPrev.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-up-icon.png"))); // NOI18N
        btnSelectPrev.setToolTipText("Navigate Up in playlist");
        btnSelectPrev.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setName("btnAddSong"); // NOI18N
        btnSelectPrev.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        btnSelectPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectPrevActionPerformed(evt);
            }
        });

        slideProgress.setMaximum(1000);
        slideProgress.setMinorTickSpacing(1);
        slideProgress.setSnapToTicks(true);
        slideProgress.setToolTipText("song progress scroller");
        slideProgress.setValue(0);
        slideProgress.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideProgressStateChanged(evt);
            }
        });

        labelSongName.setText("...");
        labelSongName.setName(""); // NOI18N

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(slideProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addComponent(labelSongName)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(panelMainLayout.createSequentialGroup()
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMainLayout.createSequentialGroup()
                            .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(33, 33, 33))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                            .addComponent(tglbtnVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(14, 14, 14)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnAddSong, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addComponent(btnSelectNext, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(slideVolume, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnSelectPrev, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(btnDeleteSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnPrevSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPlaySong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPauseSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnStopSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnNextSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addComponent(labelSongName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slideProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnDeleteSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnPlaySong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnPauseSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnStopSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnNextSong, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                    .addComponent(btnPrevSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAddSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSelectPrev))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSelectNext)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(slideVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tglbtnVolume)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelSearch.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        btnSearch.setForeground(new java.awt.Color(0,0,255));
        btnSearch.setBackground(new java.awt.Color(0,0,0));
        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/search_16.png"))); // NOI18N
        btnSearch.setToolTipText("search a song");
        btnSearch.setActionCommand("search");
        btnSearch.setName("btnSearch"); // NOI18N
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        txtSearch.setForeground(new java.awt.Color(153, 153, 153));
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtSearchFocusLost(evt);
            }
        });
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout panelSearchLayout = new javax.swing.GroupLayout(panelSearch);
        panelSearch.setLayout(panelSearchLayout);
        panelSearchLayout.setHorizontalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtSearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch)
                .addContainerGap())
        );
        panelSearchLayout.setVerticalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnSearch)
                    .addComponent(txtSearch))
                .addContainerGap())
        );

        menuFile.setText("File");

        menuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        menuOpenPlaylist.setText("Open playlist");
        menuOpenPlaylist.setName(""); // NOI18N
        menuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenPlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenPlaylist);

        menuSavePlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save_16.png"))); // NOI18N
        menuSavePlaylist.setText("Save playlist");
        menuSavePlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSavePlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuSavePlaylist);
        menuFile.add(menuSeparator);

        menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/exit.png"))); // NOI18N
        menuExit.setText("Exit");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        jMenuBar1.add(menuFile);

        menuPrefs.setText("Service");

        menuChangeSkin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/gear_16.png"))); // NOI18N
        menuChangeSkin.setText("Appearnance skins");

        menuSkin1.setText("Skin 1");
        menuSkin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin1ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin1);

        menuSkin2.setText("Skin 2");
        menuSkin2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin2ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin2);

        menuPrefs.add(menuChangeSkin);

        jMenuBar1.add(menuPrefs);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(panelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelMain, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(panelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, 349, Short.MAX_VALUE)
                .addContainerGap())
        );

        setSize(new java.awt.Dimension(369, 482));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnPlaySongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlaySongActionPerformed
        playFile();
    }//GEN-LAST:event_btnPlaySongActionPerformed

    private void btnStopSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopSongActionPerformed
        player.stop();
    }//GEN-LAST:event_btnStopSongActionPerformed

    private void btnPauseSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseSongActionPerformed
        player.pause();
    }//GEN-LAST:event_btnPauseSongActionPerformed

    private void slideVolumeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideVolumeStateChanged
        player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());

        if (slideVolume.getValue() == 0) {
            tglbtnVolume.setSelected(true);
        } else {
            tglbtnVolume.setSelected(false);
        }
    }//GEN-LAST:event_slideVolumeStateChanged

    private void btnNextSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextSongActionPerformed
        if (selectNextSong()) {
            playFile();
        }
    }//GEN-LAST:event_btnNextSongActionPerformed

    private void btnPrevSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrevSongActionPerformed
        if (selectPrevSong()) {
            playFile();
        }
    }//GEN-LAST:event_btnPrevSongActionPerformed

    private void btnSelectPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectPrevActionPerformed
//        selectPrevSong();
        int nextIndex = lstPlayList.getSelectedIndex() - 1;
        if (nextIndex >= 0){  // if we do not exceed the list in Above
            lstPlayList.setSelectedIndex(nextIndex);
        } else if (lstPlayList.getSelectedIndex() == 0){
            lstPlayList.setSelectedIndex(lstPlayList.getModel().getSize() - 1);
        }
    }//GEN-LAST:event_btnSelectPrevActionPerformed

    private void btnSelectNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectNextActionPerformed
//        selectNextSong();
        int nextIndex = lstPlayList.getSelectedIndex()+1;
        if (nextIndex <= lstPlayList.getModel().getSize() - 1){ //id dont exceed list from Below
            lstPlayList.setSelectedIndex(nextIndex);
        } else if (nextIndex == lstPlayList.getModel().getSize()){
            lstPlayList.setSelectedIndex(0);
        }
    }//GEN-LAST:event_btnSelectNextActionPerformed

    private void btnAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddSongActionPerformed
        FileUtils.addFileFilter(fileChooser, mp3FileFilter);
        int result = fileChooser.showOpenDialog(this);// result хранит результат: выбран файл или нет

        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES

            File[] selectedFiles = fileChooser.getSelectedFiles();
            // перебираем все выделенные файлы для добавления в плейлист
            for (File file : selectedFiles) {
                MP3 mp3 = new MP3(file.getName(), file.getPath());

                // если эта песня уже есть в списке - не добавлять ее
                if (!mp3ListModel.contains(mp3)) {
                    mp3ListModel.addElement(mp3);
                }
            }

        }
    }//GEN-LAST:event_btnAddSongActionPerformed

    private void btnDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteSongActionPerformed
        int[] indexPlayList = lstPlayList.getSelectedIndices();// получаем выбранные индексы(порядковый номер) песен
        if (indexPlayList.length > 0) {// если выбрали хотя бы одну песню
            ArrayList<MP3> mp3ListForRemove = new ArrayList<MP3>();// сначала сохраняем все mp3 для удаления в отдельную коллекцию
            for (int i = 0; i < indexPlayList.length; i++) {// удаляем все выбранные песни из плейлиста
                MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[i]);
                mp3ListForRemove.add(mp3);
            }

            // удаляем mp3 в плейлисте
            for (MP3 mp3 : mp3ListForRemove) {
                mp3ListModel.removeElement(mp3);
            }

        }
    }//GEN-LAST:event_btnDeleteSongActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchSong();

    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusGained
        if (txtSearch.getText().equals(INPUT_SONG_NAME)) {
            txtSearch.setText(EMPTY_STRING);
        }
    }//GEN-LAST:event_txtSearchFocusGained

    private void txtSearchFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusLost
        if (txtSearch.getText().trim().equals(EMPTY_STRING)) {
            txtSearch.setText(INPUT_SONG_NAME);
        }
    }//GEN-LAST:event_txtSearchFocusLost

    private void menuSavePlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSavePlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {// если такой файл уже существует

                int resultOvveride = JOptionPane.showConfirmDialog(this, "Файл существует", "Перезаписать?", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (resultOvveride) {
                    case JOptionPane.NO_OPTION:
                        menuSavePlaylistActionPerformed(evt);// повторно открыть окно сохранения файла
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        fileChooser.cancelSelection();
                        return;
                }
                fileChooser.approveSelection();
            }

            String fileExtension = FileUtils.getFileExtension(selectedFile);

            // имя файла (нужно ли добавлять раширение к имени файлу при сохранении)
            String fileNameForSave = (fileExtension != null && fileExtension.equals(PLAYLIST_FILE_EXTENSION)) ? selectedFile.getPath() : selectedFile.getPath() + "." + PLAYLIST_FILE_EXTENSION;

            FileUtils.serialize(mp3ListModel, fileNameForSave);
        }

    }//GEN-LAST:event_menuSavePlaylistActionPerformed

    private void menuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenPlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showOpenDialog(this);// result хранит результат: выбран файл или нет


        if (result == JFileChooser.APPROVE_OPTION) {// если нажата клавиша OK или YES
            File selectedFile = fileChooser.getSelectedFile();// 
            DefaultListModel mp3ListModel = (DefaultListModel) FileUtils.deserialize(selectedFile.getPath());
            this.mp3ListModel = mp3ListModel;
            lstPlayList.setModel(mp3ListModel);
        }


    }//GEN-LAST:event_menuOpenPlaylistActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuSkin1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin1ActionPerformed
     SkinUtils.changeSkin(this, new BernsteinLookAndFeel() );
        // set drop menu custom text instead of jtattoo default
        Properties props = new Properties();
        props.put("logoString", "LPplayer");
        BernsteinLookAndFeel.setCurrentTheme(props);
    }//GEN-LAST:event_menuSkin1ActionPerformed

    private void menuSkin2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin2ActionPerformed
        SkinUtils.changeSkin(this, new NoireLookAndFeel() );
        // set drop menu custom text instead of jtattoo default
        Properties props = new Properties();
        props.put("logoString", "LPplayer");
        NoireLookAndFeel.setCurrentTheme(props);
    }//GEN-LAST:event_menuSkin2ActionPerformed

    private void lstPlayListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstPlayListMouseClicked

        // если нажали левую кнопку мыши 2 раза
        if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListMouseClicked
    private int currentVolumeValue;
    private void tglbtnVolumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglbtnVolumeActionPerformed

        if (tglbtnVolume.isSelected()) {
            currentVolumeValue = slideVolume.getValue();
            slideVolume.setValue(0);
        } else {
            slideVolume.setValue(currentVolumeValue);
        }
    }//GEN-LAST:event_tglbtnVolumeActionPerformed

    private void popmenuAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuAddSongActionPerformed
        btnAddSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuAddSongActionPerformed

    private void popmenuDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuDeleteSongActionPerformed
        btnDeleteSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuDeleteSongActionPerformed

    private void popmenuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuOpenPlaylistActionPerformed
        menuOpenPlaylistActionPerformed(evt);
    }//GEN-LAST:event_popmenuOpenPlaylistActionPerformed

    private void popmenuClearPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuClearPlaylistActionPerformed
        mp3ListModel.clear();
    }//GEN-LAST:event_popmenuClearPlaylistActionPerformed

    private void slideProgressStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideProgressStateChanged

        if (slideProgress.getValueIsAdjusting() == false) {
            if (moveAutomatic == true) {
                moveAutomatic = false;
                posValue = slideProgress.getValue() * 1.0 / 1000;
                processSeek(posValue);
            }
        } else {
            moveAutomatic = true;
            movingFromJump = true;
        }

    }//GEN-LAST:event_slideProgressStateChanged

    private void popmenuPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPlayActionPerformed
        btnPlaySongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPlayActionPerformed

    private void popmenuStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuStopActionPerformed
        btnStopSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuStopActionPerformed

    private void popmenuPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPauseActionPerformed
        btnPauseSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPauseActionPerformed

    private void lstPlayListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_lstPlayListKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListKeyPressed

    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            searchSong();
        }
    }//GEN-LAST:event_txtSearchKeyPressed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearchActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
                                      // System Settings
         System.setProperty("sun.java2d.d3d", "false");
         System.setProperty("sun.java2d.ddoffscreen", "false");
         System.setProperty("sun.java2d.noddraw", "true");
        
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        
        try {
 

             UIManager.setLookAndFeel(new NoireLookAndFeel());
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(LPPlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        // set drop menu custom text instead of jtattoo default
        Properties props = new Properties();
        props.put("logoString", "LPplayer");
        NoireLookAndFeel.setCurrentTheme(props);
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {

        LPPlayerGui frame = new LPPlayerGui();
        // setting our icon for main frame
        ClassLoader cl = this.getClass().getClassLoader();
        URL iconURL = cl.getResource("images/icon.png");
        if (iconURL != null)
        {
            ImageIcon lpIcon = new ImageIcon(iconURL);
            frame.setIconImage(lpIcon.getImage());
        }
        //  icon is set
            frame.setVisible(true); // now we make it visible
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddSong;
    private javax.swing.JButton btnDeleteSong;
    private javax.swing.JButton btnNextSong;
    private javax.swing.JButton btnPauseSong;
    private javax.swing.JButton btnPlaySong;
    private javax.swing.JButton btnPrevSong;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSelectNext;
    private javax.swing.JButton btnSelectPrev;
    private javax.swing.JButton btnStopSong;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JLabel labelSongName;
    private javax.swing.JList lstPlayList;
    private javax.swing.JMenu menuChangeSkin;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenuItem menuOpenPlaylist;
    private javax.swing.JMenu menuPrefs;
    private javax.swing.JMenuItem menuSavePlaylist;
    private javax.swing.JPopupMenu.Separator menuSeparator;
    private javax.swing.JMenuItem menuSkin1;
    private javax.swing.JMenuItem menuSkin2;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelSearch;
    private javax.swing.JMenuItem popmenuAddSong;
    private javax.swing.JMenuItem popmenuClearPlaylist;
    private javax.swing.JMenuItem popmenuDeleteSong;
    private javax.swing.JMenuItem popmenuOpenPlaylist;
    private javax.swing.JMenuItem popmenuPause;
    private javax.swing.JMenuItem popmenuPlay;
    private javax.swing.JMenuItem popmenuStop;
    private javax.swing.JPopupMenu popupPlaylist;
    public static javax.swing.JSlider slideProgress;
    private javax.swing.JSlider slideVolume;
    private javax.swing.JToggleButton tglbtnVolume;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
